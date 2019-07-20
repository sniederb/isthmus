/**
 * Run through https://jscompress.com/
 */

$(document).ajaxSend(function(event, xhr, settings) {
	$("#userSettingsError").hide();
});

$(function() {
	isthmus.init();
});

/**
 * See https://stripe.com/docs/testing#cards
 */
var stripeHandler = StripeCheckout.configure({
  key: 'pk_live_BiK8pgZQiiFu4248X1szbEPT',
  locale: 'auto',
  zipCode: true,
  name: 'Isthmus',
  description: 'Thank you for using Isthmus',
  panelLabel: 'Get license key',
  image: 'assets/isthmus.png',
  token: function(token) {
    $('[name=stripeToken]').val(token.id);
    $('[name=stripeEmail]').val(token.email);
    $('[name=currency]').val(isthmus._getCurrency());
    isthmus.submitStripeForm();
  }
});

// filtered by maven
var isthmusVersion = '@project.version@';

var isthmus = {

	_settings : {},
	_bindingForm: false,
	_country: undefined,

	init : function() {
		this.loadSettings();
		this._updateCountry('US');
		this._setCountry();
		$("#userSettingsForm").change(function(evt) {
			isthmus.saveSettings();
		});

		$(".collapse").each(function() {
			var $this = $(this);
			var target = "#" + $this.attr("id") + "CollapseIndicator";
			$(this).on("hidden.bs.collapse", function() {
				$(target).removeClass('fa-angle-down')//
				.addClass('fa-angle-right');
			});
			$(this).on("shown.bs.collapse", function() {
				$(target).removeClass('fa-angle-right')//
				.addClass('fa-angle-down');
			});

		});
		
		$("#addNewWebhook").click(function() {
			isthmus._settings.webhookRules.push({});
			isthmus.bindSettingsToForm();
		});
		
		$("#addNewScheduledRule").click(function() {
			isthmus._settings.scheduledRules.push({});
			isthmus.bindSettingsToForm();
		});
		
		$("#refreshConsole").click(function() {
			isthmus.getLastActions();
		});
		
		$("#tryOutManualWebhook").click(function() {
			isthmus.tryOutManualWebhook();
		});
		
		$("#tryOutManualSchedule").click(function() {
			isthmus.tryOutManualSchedule();
		});
		
		$("#isthmus-version").text('Isthmus, ' + isthmusVersion);
		
		isthmus.getLastActions();
		setInterval(function(){ isthmus.getLastActions(); }, 30 * 1000);
		setInterval(function(){ isthmus.checkLicenseStatus(); }, 15 * 60 * 1000);
	},

	loadSettings : function() {
		$.ajax({
			url : 'settings',
			type : 'GET',
			dataType : "json"
		}).done(function(data) {
			isthmus._settings = data;
			isthmus._postProcessSettings();
			isthmus.bindSettingsToForm();
			isthmus.checkLicenseStatus();
		});
	},
	
	_postProcessSettings : function() {
		isthmus._settings.webhookRules = isthmus._settings.webhookRules || [];
		isthmus._settings.scheduledRules = isthmus._settings.scheduledRules || [];
		isthmus._updateTryoutSelection();
		if (isthmus._settings.licenseKey || !isthmus._settings.email) {
			$(".need-license").hide();
		}
		else {
			$(".need-license").show();
		}
	},

	checkLicenseStatus : function() {
		
		if (!this._settings.email) {
			isthmus._updateLicenseStatus(false, "Please enter your email address");
		} else {
			$.ajax({
				url : 'license',
				type : 'GET',
				dataType : "json"
			}).done(function(data) {
				var msg = isthmus._settings.licenseKey ? "Your license is valid" : "Trial-period";
				isthmus._updateLicenseStatus(true, msg);
			}).fail(function(event, xhr, thrownError) {
				var msg = isthmus._settings.licenseKey ? "That license is invalid, or in use on another Isthmus instance" : "The trial-period has ended";
				isthmus._updateLicenseStatus(false, msg);
			});
		}
	},
	
	_updateLicenseStatus : function(hasValidLicense, message) {
		if (hasValidLicense) {
			$("#licenseNeedsAttention").hide();
			$("#licenseKeyNotOk").hide();
			$("#licenseOk span").text(message);
			$("#licenseOk").show();
			$("#licenseKeyOk").show();
		} else {
			$("#licenseOk").hide();
			$("#licenseKeyOk").hide();
			$("#licenseNeedsAttention span").text(message);
			$("#licenseKeyNotOk").show();
			$("#licenseNeedsAttention").show();
		}
	},
	
	getLastActions : function() {
		$.ajax({
			url : 'console',
			type : 'GET',
			dataType : "json"
		}).done(function(data) {
		  if (data.console) {
		    $("#console").html(data.console.join('<br />'));
		  }
		});
	},
	
	/**
	 * Copy this._settings object to form controls
	 */
	bindSettingsToForm : function() {
		this._bindingForm = true;
		try {
			this._bindSettingsToForm(this._settings, "");
		}
		finally {
			this._bindingForm = false;
		}
	},
	
	_bindSettingsToForm : function(obj, prefix) {
		var targetForm = $("#userSettingsForm");
		
		$.each(obj,function(key,value) {
			var subkey = prefix ? prefix + "." + key : key;
			if (Array.isArray(value)) {
				for (var i = 0; i < value.length; i++) {
					/*
					 * Search for id="endpointsEntry2", create if not exists
					 */
					if ( ! $( "#" + key + "Entry" + i ).length ) {
						isthmus._cloneListEntry(key, i);
					}
					
					isthmus._bindSettingsToForm(value[i], subkey + '[' + i + ']');	
				}
			}
			else if (typeof value === 'object') {
				isthmus._bindSettingsToForm(value, subkey);
			}
			else if (subkey === 'enabled') {
				$("[name='" + subkey + "']", targetForm).prop('checked', value === 'true')
					.trigger("change");
			} else {
				$("[name='" + subkey + "']", targetForm).val(value)
				.trigger("change");
			}
	      });
	},
	
	/**
	 * Search for a template with id {listType}Template, clone it if needed, 
	 * and attach to the container with id {listType}List 
	 * @param listType - the JSON key of a collection, eg. 'webhookRules'
	 */
	_cloneListEntry : function(listType, index) {
		const $newContainer = $("#" + listType + "Template").clone()
			.attr("id", listType + "Entry" + index)
			.appendTo($("#" + listType + "List"))
			.show();
		// bind 'remove' event
		$("button.remove-entry", $newContainer).click(function() {
			$(this).closest(".delete-marker").remove();
		});
		// update form controls with index, eg. endpoints[2].url
		$("input, textarea, select", $newContainer).each(function() {
			var ctrl = $(this);
			ctrl.attr("name", listType + "[" + index + "]." + ctrl.attr("name"));
			if (ctrl.data("changetarget")) {
				ctrl.change(function() {
					$("[data-" + ctrl.data("changetarget") + "]", $newContainer).hide();
					$("[data-" + ctrl.data("changetarget") + "=" + ctrl.val() + "]", $newContainer).show();
				});
				ctrl.trigger("change");
			}
			
		});
	},
	
	_updateTryoutSelection : function() {
		var $sel = $("[name=tryOutCron]");
		var prevValue = $sel.val();
		$sel.empty();
		$.each(this._settings.scheduledRules, function(idx, r) {
			$('<option/>', { value : idx })//
			.text(r.title)//
			.appendTo($sel);
			
		});
		$sel.val(prevValue);
		//
		$sel = $("[name=tryOutHookname]");
		prevValue = $sel.val();
		$sel.empty();
		$.each(this._settings.webhookRules, function(idx, r) {
			$('<option/>', { value : r.hookname })//
			.text(r.title)//
			.appendTo($sel);
		});
		$sel.val(prevValue);
	},

	/**
	 * Copy form controls to this._settings, then save
	 */
	saveSettings : function() {
		if (this._bindingForm) {
			return;
		}
		var newSettings = {};
		this._addFormdataToObject($("#userSettingsForm"), newSettings);
		this._settings = newSettings;
		isthmus._postProcessSettings();
		$("#userSettingsSaved").hide();
		$("#userSettingsError").hide();

		$.ajax({
			url : 'settings',
			type : 'PUT',
			contentType: "application/json",
			dataType : "json",
			data : JSON.stringify(this._settings)
		}).done(function(data) {
			$("#userSettingsSaved").show();
			isthmus.checkLicenseStatus();
		}).fail(function(event, xhr, thrownError) {
			var msg = "Something went wrong";
			if (xhr.responseJSON) {
				msg = xhr.responseJSON.message;
			}
			else if (thrownError.message) {
				msg = thrownError.message;	
			}

			$("#errorMessage").text(msg);
			$("#userSettingsError").show();
			$("#userSettingsSaved").hide();
		});
	},
	
	_addFormdataToObject: function($baseElement, obj) {
		var arrayRegEx = /(\w+)\[(\d+)\]/;
		var match;
		$("input, textarea, select", $baseElement).each(function() {
			var ctrl = $(this);
			var pathParts = ctrl.attr("name").split(".");
			var o = obj;
			for (var i = 0; i < pathParts.length; i++) {
				if (i === (pathParts.length - 1)) {
					o[pathParts[i]] = ctrl.attr("type") === "checkbox" ? ctrl.prop("checked") : ctrl.val();
				}
				else if (match = arrayRegEx.exec(pathParts[i])) {
					var propertyName = match[1];
					var arrayIndex = parseInt(match[2]);
					if (!o[propertyName]) {
						o[propertyName] = [];
					}
					while (o[propertyName].length <= arrayIndex) {
						// we currently don't support any array of arrays
						o[propertyName].push({});
					}
					o = o[propertyName][arrayIndex];
				}
				else {
					if (!o[pathParts[i]]) {
						o[pathParts[i]] = {};
					}
					o = o[pathParts[i]];
				}
			}
		});
	},
	
	tryOutManualSchedule: function() {
		var cronRule = $("[name=tryOutCron]").val();
		$.ajax({
			url : 'triggercron/' + cronRule,
			type : 'POST',
			contentType: "application/json"
		}).done(function(data) {
			
		}).fail(function(event, xhr, thrownError) {
			var msg = "Something went wrong";
			if (xhr.responseJSON) {
				msg = xhr.responseJSON.message;
			}
			else if (thrownError.message) {
				msg = thrownError.message;	
			}
			alert(msg);
		});
	},
	
	tryOutManualWebhook: function() {
		var target = $("[name=tryOutHookname]").val();
		var payload = $("[name=tryOutPayload]").val();
		
		$.ajax({
			url : 'webhooks/' + target,
			type : 'POST',
			contentType: payload.startsWith("{") ?  "application/json" : "text/plain",
			dataType : payload.startsWith("{") ? "json" : "text",
			data : payload
		}).fail(function(event, xhr, thrownError) {
			var msg = "Something went wrong";
			if (xhr.responseJSON) {
				msg = xhr.responseJSON.message;
			}
			else if (thrownError.message) {
				msg = thrownError.message;	
			}
			alert(msg);
		});
	},
	
	_setCountry : function() {
		$.getJSON('https://json.geoiplookup.io/', function(data) {
			if (data) {
				isthmus._updateCountry(data.country_code);
			}
		});
	},
	
	_getCurrency : function() {
		if (isthmus._country === 'CH') {
			return 'CHF';
		}
		if (['AT','BE','CY','EE','FI','FR','DE','GR','IE','IT','LV','LT','LU','MT','NL','PT','SK','SI','ES'].indexOf(isthmus._country) !== -1) {
			return 'EUR';
		}
		if (isthmus._country === 'UK') {
			return 'GBP';
		}
		return 'USD';
	},
	
	/**
	 * Beware that this is does not define what is actually charged, but should match the Stripe plan setup
	 */
	_getLicenseCost : function() {
		var currency = isthmus._getCurrency();
		if (currency === 'CHF') {
			return "29.90";
		}
		if (currency === 'EUR') {
			return "24.90";
		}
		if (currency === 'GBP') {
			return "21.90";
		}
		return "32.90";
	},
	
	_updateCountry : function(newCountry) {
		isthmus._country = newCountry;
		$("#currency").text(isthmus._getCurrency());
		$("#license-cost").text(isthmus._getLicenseCost());
	},
	
	purchase : function() {
		stripeHandler.open({
			email: isthmus._settings.email,
			currency: isthmus._getCurrency()
		});
	},
	
	submitStripeForm: function(evt) {
		var frm = $('#stripeForm');
        $.ajax({
            type: frm.attr('method'),
            url: frm.attr('action'),
            data: frm.serialize()
        }).done(function(data) {
        	if (data.error) {
        		$("#licenseErrorMessage").text(data.error);
        	} else if (data.key) {
        		$("[name=licenseKey").val(data.key);
        		isthmus.saveSettings();
        	}
        }).fail(function (event, xhr, thrownError) {
        	var msg = "Something went wrong";
			if (xhr.responseJSON) {
				msg = xhr.responseJSON.message;
			}
			else if (thrownError.message) {
				msg = thrownError.message;	
			}
        	$("#licenseErrorMessage").text(msg);
        });
    
	}
}