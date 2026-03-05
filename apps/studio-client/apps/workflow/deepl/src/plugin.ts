export const initPlugin = async () => {
  const module = await import("@coremedia-labs/studio-client.shared.deepl-workflow");
  await module.addWorkflowPlugin();
};
