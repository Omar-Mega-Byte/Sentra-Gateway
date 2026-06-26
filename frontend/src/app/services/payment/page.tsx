import { ServiceConsole } from "@/components/features/service-console";
import { endpointsForGroup } from "@/lib/backend-contract";

export default function PaymentServicePage() {
  return (
    <ServiceConsole
      title="Payment Service"
      endpoints={endpointsForGroup("Payment Service")}
    />
  );
}
