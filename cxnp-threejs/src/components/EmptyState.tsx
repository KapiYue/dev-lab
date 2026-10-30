import { Icon } from "./Icon";

export function EmptyState({ title, detail, action }: { title: string; detail: string; action?: string }) {
  return <div className="empty-state" role="status">
    <Icon name="alert" />
    <strong>{title}</strong>
    <p>{detail}</p>
    {action && <code>{action}</code>}
  </div>;
}
