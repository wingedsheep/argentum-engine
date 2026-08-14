/**
 * One cube library that merges the user's account (cloud) cubes with their browser-only (localStorage)
 * cubes, tagging each with where it lives — the cube twin of {@link ./useUnifiedDecks useUnifiedDecks},
 * deliberately the same shape so the cube picker behaves like the deck picker.
 *
 * When signed in, cloud cubes are fetched in full (one `?full` request) so they carry their card lists
 * and can be used or edited without a second round-trip. A local cube whose name matches a cloud cube
 * is treated as the same cube (the cloud copy wins) so it isn't shown twice. Saves, renames and
 * deletes route to the right backing store automatically, which is what lets a guest build a cube and
 * keep it after signing in.
 */
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  type CubeDetail,
  type SharedCube,
  deleteCube as apiDeleteCube,
  listCubeDetails,
  saveCube as apiSaveCube,
  updateCube as apiUpdateCube,
} from '@/api/account'
import { type SavedCube, useCubeLibrary } from '@/store/cubeLibrary'
import { useAuthStore } from '@/store/authStore'

export interface UnifiedCube extends SavedCube {
  /** True = backed up in the account (cloud); false = browser-only (localStorage). */
  online: boolean
  /** Server id, present only for cloud cubes (used to route updates/deletes). */
  cloudId?: number
}

/** Strip the storage-location fields, leaving the wire/cloud shape. */
export function cubeToShared(cube: SavedCube): SharedCube {
  return {
    name: cube.name,
    cards: cube.cards.map((entry) => ({ name: entry.name, count: entry.count })),
    basicLandSetCode: cube.basicLandSetCode,
    packSize: cube.packSize,
  }
}

function detailToUnified(detail: CubeDetail): UnifiedCube {
  return {
    id: `cloud:${detail.id}`,
    cloudId: detail.id,
    online: true,
    name: detail.cube.name,
    cards: detail.cube.cards.map((entry) => ({ name: entry.name, count: entry.count })),
    basicLandSetCode: detail.cube.basicLandSetCode,
    packSize: detail.cube.packSize,
    updatedAt: Date.parse(detail.updatedAt) || 0,
  }
}

export function useUnifiedCubes() {
  const isLoggedIn = useAuthStore((s) => s.status === 'authenticated')
  const localCubes = useCubeLibrary((s) => s.cubes)
  const hydrate = useCubeLibrary((s) => s.hydrate)
  const hydrated = useCubeLibrary((s) => s.hydrated)
  const saveLocal = useCubeLibrary((s) => s.saveCube)
  const deleteLocal = useCubeLibrary((s) => s.deleteCube)
  const renameLocal = useCubeLibrary((s) => s.renameCube)

  const [cloud, setCloud] = useState<CubeDetail[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!hydrated) hydrate()
  }, [hydrated, hydrate])

  const reload = useCallback(() => {
    if (!isLoggedIn) {
      setCloud([])
      return
    }
    setLoading(true)
    void listCubeDetails()
      .then(setCloud)
      .catch(() => setCloud([]))
      .finally(() => setLoading(false))
  }, [isLoggedIn])

  useEffect(() => {
    reload()
  }, [reload])

  const cubes: UnifiedCube[] = useMemo(() => {
    const cloudCubes = cloud.map(detailToUnified)
    const cloudNames = new Set(cloudCubes.map((c) => c.name.toLowerCase()))
    const locals: UnifiedCube[] = localCubes
      .filter((c) => !cloudNames.has(c.name.toLowerCase()))
      .map((c) => ({ ...c, online: false }))
    return [...cloudCubes, ...locals].sort((a, b) => b.updatedAt - a.updatedAt)
  }, [cloud, localCubes])

  /**
   * Save a cube to wherever it belongs: the account when signed in (overwriting the same-named cube
   * rather than duplicating it), otherwise localStorage. `existing` routes an edit back to the copy it
   * came from even if the name changed.
   */
  const saveCube = useCallback(
    async (cube: SharedCube, existing?: UnifiedCube) => {
      if (isLoggedIn) {
        const cloudId =
          existing?.cloudId ??
          cloud.find((c) => c.name.toLowerCase() === cube.name.toLowerCase())?.id
        if (cloudId != null) await apiUpdateCube(cloudId, cube)
        else await apiSaveCube(cube)
        reload()
        return
      }
      saveLocal({
        ...(existing && existing.cloudId == null ? { id: existing.id } : {}),
        name: cube.name,
        cards: cube.cards,
        basicLandSetCode: cube.basicLandSetCode,
        packSize: cube.packSize,
      })
    },
    [isLoggedIn, cloud, reload, saveLocal],
  )

  const removeCube = useCallback(
    async (cube: UnifiedCube) => {
      if (cube.cloudId != null) {
        await apiDeleteCube(cube.cloudId)
        setCloud((prev) => prev.filter((c) => c.id !== cube.cloudId))
      } else {
        deleteLocal(cube.id)
      }
    },
    [deleteLocal],
  )

  const renameCube = useCallback(
    async (cube: UnifiedCube, name: string) => {
      if (cube.cloudId != null) {
        await apiUpdateCube(cube.cloudId, { ...cubeToShared(cube), name })
        reload()
      } else {
        renameLocal(cube.id, name)
      }
    },
    [renameLocal, reload],
  )

  return { cubes, loading, reload, saveCube, removeCube, renameCube, isLoggedIn }
}
