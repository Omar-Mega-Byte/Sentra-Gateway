import { ServiceConsole } from "@/components/features/service-console";
import { endpointsForGroup } from "@/lib/backend-contract";

export default function UserServicePage() {
  return (
    <ServiceConsole
      title="User Service"
      endpoints={endpointsForGroup("User Service")}
    />
  );
}
