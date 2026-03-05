import { sharedModules } from "@coremedia/studio-client.main.shared-modules";
import { getPluginWebpackConfig } from "@coremedia/studio-client.build-config";

export default getPluginWebpackConfig({
  name: "main_deeplWorkflowPlugin",
  sharedModules: {
    "@coremedia/studio-client.main.shared-modules": sharedModules,
  },
});
