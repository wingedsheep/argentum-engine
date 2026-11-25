package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jace, Architect of Thought — Return to Ravnica #44
 * {2}{U}{U} · Legendary Planeswalker — Jace · Loyalty 4
 *
 * - +1 is Tamiyo, Seasoned Scholar's shape: Jace's own delayed trigger lasting until your next turn
 *   ([DelayedTriggerExpiry.UntilControllersNextTurn]), firing once per creature an opponent
 *   controls as it's declared as an attacker — whatever it attacks (2012-10-01 ruling) — and giving
 *   that attacker (the triggering entity) -1/-0 until end of turn. It keeps working if Jace leaves.
 * - −2 is Fact or Fiction's divvy (CR 700.3) over the top three: an opponent splits (either pile may
 *   be empty), you pick the hand pile, and the other goes to the bottom in an order you choose.
 * - −8 iterates every player ([Effects.ForEachPlayer] rebinds "you" to the iterated player, so the
 *   library and "that player shuffles" are theirs) while Jace's controller does the searching and
 *   picking ([Player.ControllerOfSource] / [Chooser.SourceController]). "Up to one" is the fail-to-find
 *   a quality-restricted search allows. Every exile happens before any cast (2012-10-01 ruling); the
 *   collected cards are then cast during the ability's resolution, not later in the turn.
 */
val JaceArchitectOfThought = card("Jace, Architect of Thought") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 4
    oracleText = "+1: Until your next turn, whenever a creature an opponent controls attacks, it gets " +
        "-1/-0 until end of turn.\n" +
        "−2: Reveal the top three cards of your library. An opponent separates those cards into two " +
        "piles. Put one pile into your hand and the other on the bottom of your library in any order.\n" +
        "−8: For each player, search that player's library for a nonland card and exile it, then that " +
        "player shuffles. You may cast those cards without paying their mana costs."

    loyaltyAbility(+1) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).attacks(),
            effect = Effects.ModifyStats(-1, 0, EffectTarget.TriggeringEntity),
            expiry = DelayedTriggerExpiry.UntilControllersNextTurn,
        )
        description = "Until your next turn, whenever a creature an opponent controls attacks, it " +
            "gets -1/-0 until end of turn."
    }

    loyaltyAbility(-2) {
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(3, Player.You), revealed = true)
            val piles = chooseAnyNumberSplit(
                from = revealed,
                chooser = Chooser.Opponent,
                prompt = "Separate the revealed cards into two piles. The cards you select form Pile 1; the rest form Pile 2.",
                selectedLabel = "Pile 1",
                remainderLabel = "Pile 2",
                alwaysPrompt = true
            )
            val choice = choosePile(
                pileA = piles.selected,
                pileB = piles.remainder,
                chooser = Chooser.Controller,
                prompt = "Choose which pile goes to your hand; the other goes on the bottom of your library."
            )
            toHand(choice.chosen)
            toLibraryBottom(choice.other)
        }
        description = "Reveal the top three cards of your library. An opponent separates them into two " +
            "piles. Put one pile into your hand and the other on the bottom of your library in any order."
    }

    loyaltyAbility(-8) {
        effect = Effects.Pipeline {
            val (exiled) = forEachPlayerCollecting(Player.Each) {
                val nonlands = gather(
                    CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Nonland),
                    search = true,
                    searcher = Player.ControllerOfSource,
                    lookAudience = LookAudience.None
                )
                val found = chooseUpTo(
                    1,
                    from = nonlands,
                    chooser = Chooser.SourceController,
                    prompt = "Search this player's library for a nonland card to exile",
                    selectedLabel = "Exile"
                )
                exile(found)
                run(Effects.ShuffleLibrary())
                listOf(found)
            }
            run(Effects.CastAnyNumberFromCollectionWithoutPayingCost(exiled))
        }
        description = "For each player, search that player's library for a nonland card and exile it, " +
            "then that player shuffles. You may cast those cards without paying their mana costs."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "44"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4df3a38-678e-42dc-a3fd-d1d399368f07.jpg?1783940367"
        ruling(
            "2012-10-01",
            "Jace's first ability creates a delayed triggered ability that triggers whenever a creature " +
                "an opponent controls attacks. It doesn't matter which player or planeswalker that creature " +
                "is attacking."
        )
        ruling(
            "2012-10-01",
            "You pick one of your opponents when Jace's second ability resolves. The ability doesn't target " +
                "that opponent. You (not your opponent) choose which pile is put into your hand and which pile " +
                "is put on the bottom of your library."
        )
        ruling(
            "2012-10-01",
            "Piles can be empty. If one of the piles is empty, you choose to put all the revealed cards in " +
                "your hand or on the bottom of your library."
        )
        ruling(
            "2012-10-01",
            "When resolving Jace's third ability, you search each player's library (including yours) and " +
                "exile the nonland cards before casting any of them."
        )
        ruling(
            "2012-10-01",
            "If you can't cast a card, perhaps because there are no legal targets available, or if you " +
                "choose not to cast one, it will remain exiled. Jace's ability won't allow you to cast it later."
        )
        ruling(
            "2012-10-01",
            "If a card has {X} in its mana cost, you must choose 0 as its value."
        )
    }
}
