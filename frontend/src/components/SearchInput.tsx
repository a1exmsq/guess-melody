import { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Search } from 'lucide-react';
import type { Track } from '../types';

interface Props {
  onSelect: (value: string) => void;
  placeholder?: string;
  value?: string;
  onValueChange?: (value: string) => void;
}

export default function SearchInput({ onSelect, placeholder, value, onValueChange }: Props) {
  const { t } = useTranslation();
  const [query, setQuery] = useState(value || '');
  const [results, setResults] = useState<Track[]>([]);
  const [showResults, setShowResults] = useState(false);
  const timeoutRef = useRef<number | undefined>(undefined);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (value !== undefined) {
      setQuery(value);
    }
  }, [value]);

  useEffect(() => {
    if (query.length < 2) {
      setResults([]);
      return;
    }

    clearTimeout(timeoutRef.current);
    timeoutRef.current = window.setTimeout(() => {
      fetch(`/api/tracks/search?q=${encodeURIComponent(query)}`)
        .then(r => r.json())
        .then(data => {
          setResults(data);
          setShowResults(true);
        })
        .catch(() => setResults([]));
    }, 300);

    return () => clearTimeout(timeoutRef.current);
  }, [query]);

  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setShowResults(false);
      }
    };
    document.addEventListener('click', handleClick);
    return () => document.removeEventListener('click', handleClick);
  }, []);

  const handleSelect = (track: Track) => {
    onSelect(track.name);
    const newVal = track.name;
    setQuery(newVal);
    onValueChange?.(newVal);
    setShowResults(false);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      onSelect(query.trim());
      setShowResults(false);
    }
  };

  return (
    <div ref={containerRef} className="relative">
      <form onSubmit={handleSubmit} className="relative">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
        <input
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            onValueChange?.(e.target.value);
            setShowResults(true);
          }}
          placeholder={placeholder || t('search.placeholder')}
          className="w-full bg-gray-800 border border-gray-700 rounded-xl py-3 pl-12 pr-4 text-white placeholder-gray-500 focus:outline-none focus:border-brand-primary transition-colors"
        />
      </form>

      {showResults && results.length > 0 && (
        <div className="absolute z-50 w-full mt-2 bg-gray-800 rounded-xl overflow-hidden shadow-xl border border-gray-700">
          <div className="px-4 py-2 text-xs text-gray-500 border-b border-gray-700">
            {t('search.found', { count: results.length })}
          </div>
          {results.map((track) => (
            <button
              key={track.spotifyTrackId}
              onClick={() => handleSelect(track)}
              className="w-full px-4 py-3 flex items-center gap-3 hover:bg-gray-700 transition-colors text-left"
            >
              <div className="w-10 h-10 bg-gray-900 rounded flex items-center justify-center text-xs text-gray-500">
                🎵
              </div>
              <div className="flex-1 min-w-0">
                <div className="font-medium truncate">{track.name}</div>
                <div className="text-sm text-gray-500 truncate">{track.artistName}</div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
