import { ServiceConsole } from "@/components/features/service-console";
import { endpointsForGroup } from "@/lib/backend-contract";

export default function NotificationServicePage() {
  return (
    <ServiceConsole
      title="Notification Service"
      endpoints={endpointsForGroup("Notification Service")}
    />
  );
}
