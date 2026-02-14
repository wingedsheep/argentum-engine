import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Tempting Wurm.
 *
 * Card: Tempting Wurm ({1}{G}) — Creature — Wurm 5/5
 * "When Tempting Wurm enters the battlefield, each opponent may put any number of
 * artifact, creature, enchantment, and/or land cards from their hand onto the battlefield."
 *
 * Covers: EachOpponentMayPutFromHandEffect / SelectCardsDecision UI (opponent puts permanents)
 */
test.describe('Tempting Wurm', () => {
  test('opponent puts creatures and lands from hand onto the battlefield', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Tempting Wurm'],
        battlefield: [
          { name: 'Forest' },
          { name: 'Forest' },
        ],
        library: ['Forest'],
      },
      player2: {
        hand: ['Hill Giant', 'Grizzly Bears', 'Mountain'],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Tempting Wurm — spell and trigger auto-resolve for P1 (no instant-speed responses)
    await p1.clickCard('Tempting Wurm')
    await p1.selectAction('Cast Tempting Wurm')

    // P2 resolves the ETB trigger
    await p2.resolveStack('Tempting Wurm trigger')

    // Opponent sees card selection overlay — put Hill Giant and Mountain onto the battlefield
    await p2.selectCardInDecision('Hill Giant')
    await p2.selectCardInDecision('Mountain')
    await p2.confirmSelection()

    // Verify: Hill Giant and Mountain are on the battlefield, Grizzly Bears stays in hand
    await p2.expectOnBattlefield('Hill Giant')
    await p2.expectOnBattlefield('Mountain')
    await p2.expectInHand('Grizzly Bears')
    await p2.expectHandSize(1)

    await p1.expectOnBattlefield('Tempting Wurm')
    await p1.screenshot('End state — opponent put Hill Giant and Mountain onto battlefield')
  })

  test('opponent declines to put any cards onto the battlefield', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Tempting Wurm'],
        battlefield: [
          { name: 'Forest' },
          { name: 'Forest' },
        ],
        library: ['Forest'],
      },
      player2: {
        hand: ['Hill Giant', 'Grizzly Bears'],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Tempting Wurm — spell and trigger auto-resolve for P1 (no instant-speed responses)
    await p1.clickCard('Tempting Wurm')
    await p1.selectAction('Cast Tempting Wurm')

    // P2 resolves the ETB trigger
    await p2.resolveStack('Tempting Wurm trigger')

    // Opponent declines — selects nothing
    await p2.skipTargets()

    // Verify: both cards remain in hand, nothing new on opponent's battlefield
    await p2.expectHandSize(2)
    await p2.expectInHand('Hill Giant')
    await p2.expectInHand('Grizzly Bears')

    await p1.screenshot('End state — opponent declined')
  })

  test('opponent puts all cards from hand onto the battlefield', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Tempting Wurm'],
        battlefield: [
          { name: 'Forest' },
          { name: 'Forest' },
        ],
        library: ['Forest'],
      },
      player2: {
        hand: ['Glory Seeker', 'Mountain', 'Grizzly Bears'],
        library: ['Forest'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Tempting Wurm — spell and trigger auto-resolve for P1 (no instant-speed responses)
    await p1.clickCard('Tempting Wurm')
    await p1.selectAction('Cast Tempting Wurm')

    // P2 resolves the ETB trigger
    await p2.resolveStack('Tempting Wurm trigger')

    // Opponent puts everything onto the battlefield
    await p2.selectCardInDecision('Glory Seeker')
    await p2.selectCardInDecision('Mountain')
    await p2.selectCardInDecision('Grizzly Bears')
    await p2.confirmSelection()

    // Verify: all three permanents are on the battlefield, hand is empty
    await p2.expectOnBattlefield('Glory Seeker')
    await p2.expectOnBattlefield('Mountain')
    await p2.expectOnBattlefield('Grizzly Bears')
    await p2.expectHandSize(0)

    await p1.screenshot('End state — opponent put all cards onto battlefield')
  })
})
