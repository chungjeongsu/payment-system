package com.v_payment.pay;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HibernateConnectionHandlingLogger {
    private final EntityManagerFactory entityManagerFactory;

    @PostConstruct
    void logConnectionHandlingMode() {
        SessionFactoryImplementor sessionFactory =
            entityManagerFactory.unwrap(SessionFactoryImplementor.class);

        var mode = sessionFactory
            .getSessionFactoryOptions()
            .getPhysicalConnectionHandlingMode();

        log.info("connection handling mode = {}, acquisitionMode = {}, releaseMode = {}",
            mode,
            mode.getAcquisitionMode(),
            mode.getReleaseMode()
        );
    }
}
