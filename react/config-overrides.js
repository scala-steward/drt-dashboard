// Exclude drt-react from source-map-loader to silence missing source map warnings
module.exports = {
  webpack: (config) => {
    const addExclude = (rule) => {
      const drtReact = /node_modules[\\/]drt-react[\\/]/;

      if (!rule.exclude) {
        rule.exclude = [drtReact];
      } else if (Array.isArray(rule.exclude)) {
        rule.exclude = [...rule.exclude, drtReact];
      } else {
        rule.exclude = [rule.exclude, drtReact];
      }
    };

    const visitRules = (rules) => {
      rules.forEach((rule) => {
        if (rule.oneOf) visitRules(rule.oneOf);

        const uses = rule.use ? (Array.isArray(rule.use) ? rule.use : [rule.use]) : [];
        const hasSourceMapLoader =
          (rule.loader && rule.loader.includes('source-map-loader')) ||
          uses.some((use) => use && use.loader && use.loader.includes('source-map-loader'));

        if (hasSourceMapLoader) addExclude(rule);
      });
    };

    visitRules(config.module.rules);
    return config;
  },
  jest: (config) => {
    config.transformIgnorePatterns = (config.transformIgnorePatterns || []).map((pattern) =>
      pattern.includes('node_modules') ? 'node_modules/(?!(export-to-csv)/)' : pattern,
    );
    return config;
  },

  devServer: (configFunction) => {
    return (proxy, allowedHost) => {
      const shouldInjectHeaders = process.env.INJECT_TEST_HEADERS === "true";
      const testHeaders = {
        "X-Forwarded-Email": "test@example.com",
        "X-Forwarded-Groups":
            "role:forecast:view,role:national:view,role:fixed-points:view,role:staff:edit,role:TEST,role:EXT,role:terminal-dashboard,role:egate-banks:edit,role:BRS,role:desks-and-queues:view,role:manage-users,role:view-config,role:LBA,role:sla-configs:edit,role:EDI,role:STN,role:BFS,role:health-checks:edit,role:rcc:north,role:DSA,role:super-admin,role:EMA,role:staff:edit,role:BOH,role:uma_authorization,role:port-feed-upload,role:NWI,role:red-list-feature,role:api:view,role:MME,role:iei-dashboard:view,role:rcc:central,role:staff-movements:export,role:border-force-staff,role:NCL,role:default-roles-drt-prod,role:red-lists:edit,role:arrivals-and-splits:view,role:MAN,role:SEN,role:LGW,role:rcc:south,role:BHD,role:LCY,role:create-alerts,role:arrival-simulation-upload,role:LTN,role:LPL,role:INV,role:BHX,role:rcc:heathrow,role:enhanced-api-view,role:national:view,role:offline_access,role:LHR,role:forecast:view,role:CWL,role:ABZ,role:download-manager,role:debug,role:HUY,role:staff-movements:edit,role:PIK,role:arrival-source,role:fixed-points:view,role:NQY,role:SOU,role:GLA,role:account:manage-account,role:account:manage-account-links,role:account:view-profile,role:realm-management:view-realm,role:realm-management:view-identity-providers,role:realm-management:manage-identity-providers,role:realm-management:impersonation,role:realm-management:realm-admin,role:realm-management:create-client,role:realm-management:manage-users,role:realm-management:view-authorization,role:realm-management:query-clients,role:realm-management:query-users,role:realm-management:manage-events,role:realm-management:manage-realm,role:realm-management:view-events,role:realm-management:view-users,role:realm-management:view-clients,role:realm-management:manage-authorization,role:realm-management:manage-clients,role:realm-management:query-groups",
      }
      const config = configFunction(proxy, allowedHost);
      config.proxy = {
        ...config.proxy,
        "/api": {
          target: "http://localhost:8081",
          changeOrigin: true,
          ws: true,
          headers: shouldInjectHeaders ? testHeaders : undefined,
        }
      }
      return config;
    }
  }
};
