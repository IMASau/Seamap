module.exports = {
  "stories": [
    "../src/ui/**/*.stories.mdx",
    "../src/ui/**/*.stories.@(js|jsx|ts|tsx)"
  ],
  "addons": [
    "@storybook/addon-links",
    "@storybook/addon-essentials"
  ],
  webpackFinal: async (config, { configType }) => {
    // more configuration options
    config.module.rules.push({
      test: /\.(js|jsx)$/,
      loader: require.resolve("babel-loader"),
      options: {
        presets: [
          '@babel/preset-env',
          '@babel/preset-react',
        ],
        plugins: [
          "@babel/plugin-proposal-nullish-coalescing-operator",
        ],
      },
    });
    return config;
  },
}
