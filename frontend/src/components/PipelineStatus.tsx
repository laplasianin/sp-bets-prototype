import { useEffect, useState } from 'react';

const STAGES = ['API', 'Kafka', 'Settlement', 'DB'];
const STAGE_DELAY_MS = 500;

interface Props {
  active: boolean;
}

export function PipelineStatus({ active }: Props) {
  const [step, setStep] = useState(-1);

  useEffect(() => {
    if (!active) {
      setStep(-1);
      return;
    }
    const timers = STAGES.map((_, i) =>
      setTimeout(() => setStep(i), i * STAGE_DELAY_MS),
    );
    const done = setTimeout(() => setStep(STAGES.length), STAGES.length * STAGE_DELAY_MS);
    return () => {
      timers.forEach(clearTimeout);
      clearTimeout(done);
    };
  }, [active]);

  return (
    <div className="flex items-center gap-2">
      {STAGES.map((stage, i) => (
        <div key={stage} className="flex items-center gap-2">
          <span
            className={`px-3 py-1 rounded-full text-sm font-medium transition-colors duration-300 ${
              step === i
                ? 'bg-blue-600 text-white'
                : step > i
                ? 'bg-green-500 text-white'
                : 'bg-gray-200 text-gray-500'
            }`}
          >
            {stage}
          </span>
          {i < STAGES.length - 1 && (
            <span className={`font-bold transition-colors ${step > i ? 'text-green-500' : 'text-gray-300'}`}>
              →
            </span>
          )}
        </div>
      ))}
    </div>
  );
}
