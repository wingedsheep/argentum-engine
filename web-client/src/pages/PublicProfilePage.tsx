/**
 * Read-only public profile for any player, at /u/:userId. Fetches the bundled
 * `/api/stats/users/{id}` response and renders the shared {@link StatsDashboard} (plus a compact
 * recent-games list). No auth required — profiles are public — and decklists are never exposed here
 * (the deck viewer stays on the owner's own profile).
 *
 * When the viewer is signed in and looking at someone else, a friend control is offered. The
 * relationship is derived entirely from the already-loaded friends store (friends / outgoing /
 * incoming lists are all keyed by account id, which equals this profile's userId), so no extra
 * request or endpoint is needed — sending or accepting reuses the same store actions as the friends
 * page. A Message button opens the conversation with them (a message request if you aren't friends).
 */
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { type PublicProfile, ProfileNotFoundError, fetchPublicProfile } from '@/api/account'
import { StatsDashboard } from '@/components/profile/StatsDashboard'
import { AccountPage, Avatar, MessageCard, accountStyles as a } from '@/components/profile/accountUi'
import { pageStyles as p } from '@/components/ui/PageShell'
import { useAuthStore } from '@/store/authStore'
import { useFriendsStore } from '@/store/friendsStore'

export function PublicProfilePage() {
  const navigate = useNavigate()
  const { userId } = useParams<{ userId: string }>()
  const [profile, setProfile] = useState<PublicProfile | null>(null)
  const [state, setState] = useState<'loading' | 'ready' | 'notfound' | 'error'>('loading')

  useEffect(() => {
    if (!userId) return
    let live = true
    setState('loading')
    fetchPublicProfile(userId)
      .then((p) => {
        if (!live) return
        setProfile(p)
        setState('ready')
      })
      .catch((e) => {
        if (!live) return
        setState(e instanceof ProfileNotFoundError ? 'notfound' : 'error')
      })
    return () => {
      live = false
    }
  }, [userId])

  // Friend control — only meaningful for a signed-in viewer looking at someone else's profile.
  const authUser = useAuthStore((s) => s.user)
  const authStatus = useAuthStore((s) => s.status)
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)
  const initAuth = useAuthStore((s) => s.init)
  const friends = useFriendsStore((s) => s.friends)
  const incoming = useFriendsStore((s) => s.incoming)
  const outgoing = useFriendsStore((s) => s.outgoing)
  const loadFriends = useFriendsStore((s) => s.load)
  const sendRequest = useFriendsStore((s) => s.sendRequest)
  const acceptRequest = useFriendsStore((s) => s.accept)

  const [friendBusy, setFriendBusy] = useState(false)
  const [friendError, setFriendError] = useState<string | null>(null)

  useEffect(() => {
    if (authStatus === 'idle') void initAuth()
  }, [authStatus, initAuth])

  // Pull the friends/requests lists once we know we're signed in, so the relationship is current
  // even when landing straight on this page (they may already be loaded from sign-in).
  useEffect(() => {
    if (authStatus === 'authenticated') void loadFriends()
  }, [authStatus, loadFriends])

  const isOwnProfile = !!authUser && authUser.id === userId
  const canBefriend =
    authStatus === 'authenticated' && accountsEnabled && !isOwnProfile && !!userId
  const isFriend = friends.some((f) => f.accountId === userId)
  const incomingRequest = incoming.find((r) => r.accountId === userId)
  const outgoingPending = outgoing.some((r) => r.accountId === userId)

  const addFriend = async () => {
    if (!userId) return
    setFriendBusy(true)
    setFriendError(null)
    try {
      await sendRequest(userId)
    } catch (e) {
      setFriendError(e instanceof Error ? e.message : 'Could not send the request.')
    } finally {
      setFriendBusy(false)
    }
  }

  const acceptFriend = async () => {
    if (!incomingRequest) return
    setFriendBusy(true)
    setFriendError(null)
    try {
      await acceptRequest(incomingRequest.requestId)
    } catch (e) {
      setFriendError(e instanceof Error ? e.message : 'Could not accept the request.')
    } finally {
      setFriendBusy(false)
    }
  }

  const title = state === 'ready' && profile ? profile.displayName : 'Player'

  return (
    <AccountPage title={title} width="wide" plain>
      {state === 'loading' && (
        <MessageCard center>
          <span className={a.spinner} aria-hidden />
          <p className={a.muted}>Loading profile…</p>
        </MessageCard>
      )}
      {(state === 'notfound' || state === 'error') && (
        <MessageCard>
          <h1 className={p.h1}>{state === 'notfound' ? 'Player not found' : 'Couldn’t load profile'}</h1>
          <p className={a.muted}>
            {state === 'notfound'
              ? 'This profile doesn’t exist or is no longer available.'
              : 'Something went wrong fetching this player’s stats.'}
          </p>
          <button type="button" className={p.button} onClick={() => navigate(-1)}>
            ← Go back
          </button>
        </MessageCard>
      )}
      {state === 'ready' && profile && (
        <>
          <section className={p.panel}>
            <div className={a.identity}>
              <Avatar name={profile.displayName} avatar={profile.avatar} />
              <div className={a.identityText}>
                <h1 className={p.h1}>{profile.displayName}</h1>
                <p className={a.muted}>
                  {isOwnProfile ? 'This is how others see your profile' : 'Player profile'}
                  {profile.stats.games > 0 && ` · ${profile.stats.games} game${profile.stats.games === 1 ? '' : 's'} played`}
                </p>
              </div>
              {canBefriend && (
                <div className={a.identityActions}>
                  <button type="button" className={p.button} onClick={() => navigate(`/messages/${userId}`)}>
                    Message
                  </button>
                  {isFriend ? (
                    <span className={a.badgeFriends}>✓ Friends</span>
                  ) : incomingRequest ? (
                    <button type="button" className={p.buttonPrimary} disabled={friendBusy} onClick={() => void acceptFriend()}>
                      {friendBusy ? 'Accepting…' : 'Accept friend request'}
                    </button>
                  ) : outgoingPending ? (
                    <span className={a.badgePending}>Friend request sent</span>
                  ) : (
                    <button type="button" className={p.buttonPrimary} disabled={friendBusy} onClick={() => void addFriend()}>
                      {friendBusy ? 'Sending…' : '+ Add friend'}
                    </button>
                  )}
                </div>
              )}
            </div>
            {friendError && <p className={a.error} style={{ marginTop: 10 }}>{friendError}</p>}
          </section>
          <StatsDashboard
            stats={profile.stats}
            ratings={profile.ratings}
            ratingHistory={profile.ratingHistory}
            colors={profile.colors}
            cardTypes={profile.cardTypes}
            curve={profile.curve}
            creatureTypes={profile.creatureTypes}
            modes={profile.modes}
            sets={profile.sets}
            topCards={profile.topCards}
            opponents={profile.opponents}
            tournaments={profile.tournaments}
            recentGames={profile.recentGames}
          />
        </>
      )}
    </AccountPage>
  )
}
