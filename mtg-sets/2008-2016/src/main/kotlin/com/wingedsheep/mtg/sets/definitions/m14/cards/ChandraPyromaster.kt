package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Zone

/**
 * Chandra, Pyromaster — Magic 2014 #132 (canonical printing)
 * {2}{R}{R} · Legendary Planeswalker — Chandra · Loyalty 4
 *
 * +1: Chandra deals 1 damage to target player or planeswalker and 1 damage to up to one target
 *     creature that player or that planeswalker's controller controls. That creature can't block
 *     this turn.
 * 0: Exile the top card of your library. You may play it this turn.
 * −7: Exile the top ten cards of your library. Choose an instant or sorcery card exiled this way
 *     and copy it three times. You may cast the copies without paying their mana costs.
 *
 * - +1 is Ravager of the Fells' dependent second target ("that player or that planeswalker's
 *   controller"), plus a this-turn can't-block on the same creature — which applies even when the
 *   damage is prevented (ruling).
 * - 0 is the one-card impulse draw ([Patterns.Exile.impulse]): play, so a land may be played under
 *   the normal land-play rules (ruling).
 * - −7 exiles the ten (tracked, so "exiled this way" is only this resolution's cards), chooses one
 *   instant or sorcery among them, copies it three times in exile (each copy created where the
 *   original is, CR 707.12) and offers each copy as a free cast during the resolution, so timing
 *   restrictions are ignored. The card itself stays exiled and is never cast; an uncast copy
 *   ceases to exist (CR 707.10a).
 */
val ChandraPyromaster = card("Chandra, Pyromaster") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 4
    oracleText = "+1: Chandra deals 1 damage to target player or planeswalker and 1 damage to up to one " +
        "target creature that player or that planeswalker's controller controls. That creature can't " +
        "block this turn.\n" +
        "0: Exile the top card of your library. You may play it this turn.\n" +
        "−7: Exile the top ten cards of your library. Choose an instant or sorcery card exiled this way " +
        "and copy it three times. You may cast the copies without paying their mana costs."

    loyaltyAbility(+1) {
        val recipient = target(Targets.PlayerOrPlaneswalker)
        // "That player or that planeswalker's controller": the first branch reads a player
        // target, the second the controller of a planeswalker target (Ravager of the Fells).
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.targetPlayerControls(recipient) or
                    GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController),
            ),
            optional = true,
        )
        effect = Effects.DealDamage(1, recipient) then
            Effects.DealDamage(1, creature) then
            Effects.CantBlock(creature)
        description = "Chandra deals 1 damage to target player or planeswalker and 1 damage to up to one " +
            "target creature that player or that planeswalker's controller controls. That creature can't " +
            "block this turn."
    }

    loyaltyAbility(0) {
        effect = Patterns.Exile.impulse(1)
        description = "Exile the top card of your library. You may play it this turn."
    }

    loyaltyAbility(-7) {
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(10))
            val exiled = moveTracked(top, CardDestination.ToZone(Zone.EXILE))
            val chosen = chooseExactly(
                1,
                from = exiled,
                filter = GameObjectFilter.InstantOrSorcery,
                prompt = "Choose an instant or sorcery card to copy three times",
            )
            // Three copies of the chosen card, each offered as its own free cast. The card itself
            // stays in exile, so every copy is made from it (CR 707.12).
            ifNotEmpty(chosen) {
                for (i in 1..3) {
                    val copy = copyCards(chosen)
                    run(Effects.May(
                        Effects.CastFromCollectionWithoutPayingCost(copy),
                        descriptionOverride = "You may cast copy $i of 3 without paying its mana cost.",
                    ))
                }
            }
        }
        description = "Exile the top ten cards of your library. Choose an instant or sorcery card exiled " +
            "this way and copy it three times. You may cast the copies without paying their mana costs."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "132"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcb4f983-a4b4-46df-830d-ab3d892c93bb.jpg?1783939916"

        ruling("2014-07-18", "To activate the first ability, you must target a player but you can choose to not target any creatures.")
        ruling("2014-07-18", "If the first ability resolves but the damage is prevented or redirected, the target creature still won't be able to block that turn.")
        ruling("2014-07-18", "The card exiled by the second ability is exiled face up. Playing it follows the normal rules for playing that card. You must pay its costs, and you must follow all applicable timing rules. For example, if it's a creature card, you can cast it only during your main phase while the stack is empty.")
        ruling("2014-07-18", "If you exile a land card using the second ability, you may play that land only if you have any available land plays. Normally, this means you can play the land only if you haven't played a land yet that turn.")
        ruling("2014-07-18", "In the third ability, \"exiled this way\" means the cards you just exiled because of that ability resolving. Cards exiled with the second ability or with previous activations of the third ability don't count.")
        ruling("2014-07-18", "While resolving the third ability, you create the copies of the card in exile and cast them from exile. You can cast zero, one, two, or all three copies. The card itself isn't cast. It remains exiled.")
        ruling("2014-07-18", "While casting the copies created by the third ability, timing restrictions based on the card's type (such as creature or sorcery) are ignored. Other restrictions, such as \"Cast [this spell] only during combat,\" are not.")
        ruling("2014-07-18", "Because you're casting the copies \"without paying their mana costs,\" you can't pay any alternative costs for the copies. You can pay additional costs, such as kicker costs. If the copies have any mandatory additional costs, you must pay those.")
        ruling("2014-07-18", "If the copied card has {X} in its mana cost, you must choose 0 as its value when casting the copies.")
    }
}
