package org.eluder.logback.ext.aws.core;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;

public class AwsSupport {

    public AWSCredentialsProvider getCredentials() {
        return getCredentials(null);
    }

    public AWSCredentialsProvider getCredentials(AWSCredentials credentials) {
        return new AWSCredentialsProviderChain(
                new EnvironmentVariableCredentialsProvider(),
                new SystemPropertiesCredentialsProvider(),
                new StaticCredentialsProvider(credentials == null ? new NullCredentials() : credentials),
                new ProfileCredentialsProvider(),
                new InstanceProfileCredentialsProvider()
        );
    }

    public ClientConfiguration getClientConfiguration() {
        return new ClientConfiguration();
    }

    private static class NullCredentials implements AWSCredentials {
        @Override
        public String getAWSAccessKeyId() {
            return null;
        }

        @Override
        public String getAWSSecretKey() {
            return null;
        }
    }
}
