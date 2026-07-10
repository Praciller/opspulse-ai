package com.opspulse.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.opspulse", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule domain_remains_framework_and_adapter_independent =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..api..", "..infrastructure..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_does_not_depend_on_driving_or_driven_adapters =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..api..", "..infrastructure..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule shared_code_does_not_depend_on_features =
            noClasses()
                    .that()
                    .resideInAPackage("com.opspulse.shared..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.opspulse.identity..",
                            "com.opspulse.product..",
                            "com.opspulse.supplier..",
                            "com.opspulse.order..",
                            "com.opspulse.inventory..",
                            "com.opspulse.purchase..",
                            "com.opspulse.risk..",
                            "com.opspulse.ai..",
                            "com.opspulse.importjob..",
                            "com.opspulse.audit..",
                            "com.opspulse.outbox..")
                    .allowEmptyShould(true);
}
