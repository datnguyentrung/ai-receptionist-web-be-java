package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.context.TenantContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Bọc DataSource để mỗi connection lấy ra cho transaction đều thiết lập
 * {@code set_config('app.tenant_id', ..., true)} (transaction-local) trên PostgreSQL.
 * Khi thiếu TenantContext, reset về rỗng để đảm bảo fail-closed và không rò rỉ từ pool.
 */
@Configuration
public class TenantDataSourceConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
            return new TenantAwareDataSource(dataSource);
        }
        return bean;
    }

    static final class TenantAwareDataSource extends DelegatingDataSource {
        TenantAwareDataSource(DataSource targetDataSource) {
            super(targetDataSource);
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection connection = super.getConnection();
            applyTenantSessionSetting(connection);
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection connection = super.getConnection(username, password);
            applyTenantSessionSetting(connection);
            return connection;
        }

        private void applyTenantSessionSetting(Connection connection) throws SQLException {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                return;
            }
            UUID tenantId = TenantContext.getTenantIdOrNull();
            String value = tenantId != null ? tenantId.toString() : "";
            try (PreparedStatement ps = connection.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                ps.setString(1, value);
                ps.execute();
            }
        }
    }
}
