export default function BackgroundShapes() {
  return (
    <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
      <div
        className="absolute -top-[20%] -left-[10%] w-[70vw] h-[70vw] rounded-full opacity-20 blur-[120px] animate-blob"
        style={{ background: 'radial-gradient(circle, #6366f1 0%, transparent 70%)' }}
      />
      <div
        className="absolute top-[30%] -right-[15%] w-[60vw] h-[60vw] rounded-full opacity-15 blur-[140px] animate-blob animation-delay-2000"
        style={{ background: 'radial-gradient(circle, #8b5cf6 0%, transparent 70%)' }}
      />
      <div
        className="absolute -bottom-[10%] left-[20%] w-[50vw] h-[50vw] rounded-full opacity-15 blur-[120px] animate-blob animation-delay-4000"
        style={{ background: 'radial-gradient(circle, #ec4899 0%, transparent 70%)' }}
      />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-slate-900 via-gray-950 to-black opacity-80" />
    </div>
  );
}
