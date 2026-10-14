/**
 * `/cubes/shared/:code` — a cube someone shared by link.
 *
 * Laid out like the deckbuilder's Deck view, since reading a list is what a friend came to do: the
 * hovered card large in the left rail, the cube in flowing columns grouped by type, colour or rarity,
 * and its balance numbers along the bottom. Read-only, and always the owner's latest saved version.
 * What a friend wants from it is to look, then either keep a copy (into their own library —
 * localStorage for a guest, so no account is needed) or play it straight away. "Copy list" covers
 * everything else.
 */
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { type SharedCubeView, getSharedCube } from '@/api/account'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { CubeColumns, CubeColumnsToolbar, CubeRailPreview, useCubeColumns } from '@/components/cube/CubeColumns'
import { CubePlayButton } from '@/components/cube/CubePlayButton'
import { CubeStats, PackStrip } from '@/components/cube/CubeStats'
import { boosterStyleInfo, cubeToText, isBoosterStyle, uniqueCubeName } from '@/components/cube/cubeModel'
import { useCardCatalog } from '@/components/cube/useCardCatalog'
import { PageShell, pageStyles as p } from '@/components/ui/PageShell'
import { useHoverStore } from '@/components/ui/hoverStore'
import { cubeCardCount } from '@/store/cubeLibrary'
import { type UnifiedCube, cubeToShared, useUnifiedCubes } from '@/store/useUnifiedCubes'
import db from '@/components/deckbuilder/deckbuilder.module.css'
import styles from '@/components/cube/SharedCube.module.css'

type Load =
  | { state: 'loading' }
  | { state: 'missing' }
  | { state: 'error'; message: string }
  | { state: 'ready'; view: SharedCubeView }

export function SharedCubePage() {
  const { code = '' } = useParams<{ code: string }>()
  const [load, setLoad] = useState<Load>({ state: 'loading' })

  useEffect(() => {
    let cancelled = false
    setLoad({ state: 'loading' })
    getSharedCube(code)
      .then((view) => {
        if (!cancelled) setLoad(view ? { state: 'ready', view } : { state: 'missing' })
      })
      .catch((err: unknown) => {
        if (!cancelled) setLoad({ state: 'error', message: err instanceof Error ? err.message : 'Could not load the cube' })
      })
    return () => {
      cancelled = true
    }
  }, [code])

  if (load.state === 'ready') {
    return (
      <PageShell title="Shared cube" fit width="full" plain>
        <SharedCube view={load.view} />
      </PageShell>
    )
  }
  return (
    <PageShell title="Shared cube" width="wide">
      {load.state === 'loading' && <p className={p.empty}>Loading cube…</p>}
      {(load.state === 'missing' || load.state === 'error') && <NotFound message={load.state === 'error' ? load.message : null} />}
    </PageShell>
  )
}

function NotFound({ message }: { message: string | null }) {
  const navigate = useNavigate()
  return (
    <div className={p.panel}>
      <h1 className={p.h1}>This cube link doesn’t open</h1>
      <p className={p.lede}>
        {message ?? 'Its owner may have stopped sharing it or deleted the cube. Ask them for a new link.'}
      </p>
      <p>
        <button type="button" className={p.button} onClick={() => navigate('/library?tab=cubes')}>
          Go to your cubes
        </button>
      </p>
    </div>
  )
}

function SharedCube({ view }: { view: SharedCubeView }) {
  const navigate = useNavigate()
  const catalog = useCardCatalog()
  const library = useUnifiedCubes()
  const [copied, setCopied] = useState<UnifiedCube | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const hover = useHoverStore<CardSummary>()

  const cube = view.cube
  const lookup = useMemo(
    () => (name: string) => catalog.byName.get(name.toLowerCase()),
    [catalog.byName],
  )
  const total = cubeCardCount(cube.cards)
  const unresolved = catalog.ready ? cube.cards.filter((e) => !lookup(e.name)).length : 0
  const style = cube.boosterStyle ?? 'SHUFFLED'
  const rarityMatters = isBoosterStyle(style)

  const columns = useCubeColumns(cube.cards, lookup, catalog.ready)
  const { groups } = columns

  // The rail is never blank: until something is hovered, it shows the cube's first card.
  const firstCard = useMemo(() => {
    const first = groups[0]?.entries[0]
    return first ? lookup(first.name) ?? null : null
  }, [groups, lookup])

  async function saveCopy() {
    setBusy(true)
    setNotice(null)
    try {
      const name = uniqueCubeName(cube.name, library.cubes.map((c) => c.name))
      const saved = await library.saveCube({ ...cubeToShared(cube), name })
      setCopied(saved)
    } catch (err) {
      setNotice(err instanceof Error ? err.message : 'Could not save a copy')
    } finally {
      setBusy(false)
    }
  }

  async function copyList() {
    try {
      await navigator.clipboard.writeText(cubeToText(cube))
      setNotice('Cube list copied')
    } catch {
      setNotice('Your browser blocked the clipboard.')
    }
  }

  return (
    <div className={`${db.workspace} ${db.workspaceDeckMode}`}>
      <aside className={db.left}>
        <CubeRailPreview hover={hover} fallback={firstCard} />
        {rarityMatters && (
          <div className={styles.packCard}>
            <span className={styles.packLabel}>Each pack</span>
            <PackStrip style={style} />
            <span className={styles.packBlurb}>{boosterStyleInfo(style).blurb}</span>
          </div>
        )}
      </aside>

      <main className={db.centerDeck}>
        <header className={styles.header}>
          <div className={styles.titleBlock}>
            <h1 className={styles.title}>{cube.name}</h1>
            <span className={styles.byline}>
              {view.ownerName ? `Shared by ${view.ownerName}` : 'A shared cube'} · {total} cards ·{' '}
              {boosterStyleInfo(style).label}
            </span>
            {cube.description && <p className={styles.description}>{cube.description}</p>}
          </div>
          <div className={styles.actions}>
            <CubePlayButton
              cube={cube}
              blockedReason={unresolved > 0 ? `${unresolved} cards in this cube aren’t implemented yet` : null}
            />
            {copied ? (
              <button
                type="button"
                className={p.button}
                onClick={() => navigate(`/cubes/edit/${encodeURIComponent(copied.id)}`)}
              >
                Open your copy
              </button>
            ) : (
              <button type="button" className={p.button} disabled={busy || !library.ready} onClick={() => void saveCopy()}>
                {busy ? 'Saving…' : 'Save a copy'}
              </button>
            )}
            <button type="button" className={p.buttonGhost} onClick={() => void copyList()}>
              Copy list
            </button>
          </div>
        </header>

        {(copied || notice) && (
          <p className={styles.notice} role="status">
            {copied
              ? `Saved as “${copied.name}” ${copied.online ? 'in your account' : 'on this device'}. Your copy won’t change when the original does.`
              : notice}
          </p>
        )}

        <CubeColumnsToolbar state={columns} total={cube.cards.length} />

        <CubeColumns
          groups={groups}
          filter={columns.filter}
          lookup={lookup}
          catalogReady={catalog.ready}
          rarityMatters={rarityMatters}
          hover={hover}
          fallbackName={firstCard?.name ?? null}
        />

        <div className={styles.footer}>
          <CubeStats entries={cube.cards} lookup={lookup} boosterStyle={style} packSize={cube.packSize} />
        </div>
      </main>
    </div>
  )
}
