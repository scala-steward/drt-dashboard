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
};
