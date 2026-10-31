package com.pm.infra;

import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awscdk.BootstraplessSynthesizer;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * InfraApp — The entry point for our AWS CDK infrastructure.
 *
 * <p>In an industry-standard setup, the App class is responsible for the deployment context. It
 * decides WHICH environment (account, region, synthesizer) the Stack will be deployed to. The Stack
 * itself remains completely unaware of its environment.
 */
public class InfraApp {

    public static void main(String[] args) {
        // App is the root of the CDK construct tree.
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        // We use the BootstraplessSynthesizer because LocalStack doesn't require
        // the standard AWS CDK bootstrap S3 bucket.
        // If we were deploying to a real AWS production account, we would use the default
        // synthesizer.
        StackProps props =
                StackProps.builder()
                        .env(
                                Environment.builder()
                                        .account("000000000000") // LocalStack default account
                                        .region("us-east-1") // LocalStack default region
                                        .build())
                        .synthesizer(BootstraplessSynthesizer.Builder.create().build())
                        .build();

        // Instantiate our main infrastructure stack, passing the context props.
        new PatientManagementStack(app, "PatientManagementStack", props);

        // Synthesize the CloudFormation template.
        app.synth();
    }
}
