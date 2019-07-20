/*
 * Created on 18 Jul 2018
 */
package ch.want.devtaskhub.state;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.http.HttpMethod;

public class Endpoint {

    private String url;
    private String httpMethod;
    private String username;
    private String password;

    public Endpoint() {
    }

    public Endpoint(final String url, final String httpMethod, final String username, final String password) {
        super();
        this.url = url;
        this.httpMethod = httpMethod;
        this.username = username;
        this.password = password;
    }

    public Endpoint(final String url, final String username, final String password) {
        this(url, HttpMethod.GET.toString(), username, password);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)//
                .append("url", url)//
                .build();
    }
}