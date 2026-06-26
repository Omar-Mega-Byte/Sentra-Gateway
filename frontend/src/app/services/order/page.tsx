import { ServiceConsole } from "@/components/features/service-console";
import { endpointsForGroup } from "@/lib/backend-contract";

export default function OrderServicePage() {
  return (
    <ServiceConsole
      title="Order Service"
      endpoints={endpointsForGroup("Order Service")}
    />
  );
}
