module.exports = {
  // set your styleguidist configuration here
  title: "MOLGENIS EMX2 Style Guide",
  theme: {
    color: {
      ribbonBackground: "black",
    },
    maxWidth: "auto",
  },
  assetsDir: "public",
  webpackConfig: {
    devServer: {
      proxy: {
        "/graphql": "http://localhost:8080/CohortsCentral/graphql",
        "**/graphql": "http://localhost:8080/",
        "/apps": "http://localhost:8080",
      },
    },
  },
  tocMode: "collapse",
  exampleMode: "collapse",
  template: {
    head: {
      links: [
        {
          rel: "stylesheet",
          href: "https://fonts.googleapis.com/css?family=Oswald:500|Roboto|Roboto+Mono&display=swap",
        },
        {
          rel: "stylesheet",
          href: "/apps/central/theme.css",
        },
      ],
    },
  },
  styleguideDir: "build",
  sections: [
    {
      name: "Introduction",
      content: "src/styleguide/introduction.md",
    },
    {
      name: "Form",
      components: "src/forms/[A-Z]*.vue",
    },
    {
      name: "Table",
      components: "src/tables/[A-Z]*.vue",
    },
    {
      name: "Layout",
      components: "src/layout/[A-Z]*.vue",
    },
    {
      name: "Mixins",
      components: "src/mixins/[A-Z]*.vue",
    },
    {
      name: "Tree",
      components: "src/tree/[A-Z]*.vue",
    },
  ],
};
