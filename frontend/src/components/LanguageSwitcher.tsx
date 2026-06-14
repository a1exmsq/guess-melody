import { useTranslation } from 'react-i18next';
import { Globe } from 'lucide-react';

export default function LanguageSwitcher() {
  const { i18n, t } = useTranslation();

  const toggleLanguage = () => {
    const next = i18n.language === 'ru' ? 'en' : 'ru';
    i18n.changeLanguage(next);
  };

  return (
    <button
      onClick={toggleLanguage}
      className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-brand-surface border border-brand-border text-xs font-medium text-brand-text hover:bg-brand-border transition-colors"
      title={t('language.title')}
    >
      <Globe className="w-4 h-4 text-brand-primary" />
      <span>{i18n.language === 'ru' ? t('language.ru') : t('language.en')}</span>
    </button>
  );
}
