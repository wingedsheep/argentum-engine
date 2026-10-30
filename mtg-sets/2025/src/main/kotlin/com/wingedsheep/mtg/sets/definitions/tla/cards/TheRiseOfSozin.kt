package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.scripting.targets.TotalCap

/**
 * The Rise of Sozin // Fire Lord Sozin (TLA #117)
 * {4}{B}{B} — Enchantment — Saga
 * //  — Legendary Creature — Human Noble 5/5
 *
 * Front — The Rise of Sozin:
 *   (As this Saga enters and after your draw step, add a lore counter.)
 *   I — Destroy all creatures.
 *   II — Choose a card name. Search target opponent's graveyard, hand, and library for up to four
 *        cards with that name and exile them. Then that player shuffles.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Fire Lord Sozin:
 *   Menace, firebending 3 (Whenever this creature attacks, add {R}{R}{R}. This mana lasts until end
 *   of combat.)
 *   Whenever Fire Lord Sozin deals combat damage to a player, you may pay {X}. When you do, put any
 *   number of target creature cards with total mana value X or less from that player's graveyard onto
 *   the battlefield under your control.
 *
 * Chapter I is the standard board wipe ([Effects.DestroyAll] over [GameObjectFilter.Creature]).
 * Chapter II is the "name a card, then multi-zone search" family (Lobotomy / Desperate Research):
 * [Effects.ChooseCardName] records a free-typed name, a [CardSource.FromMultipleZones] gather over
 * the target opponent's graveyard/hand/library filtered by [GameObjectFilter.namedFromVariable]
 * finds every match, a `ChooseUpTo(4)` select caps it at four, the picks are exiled, and the opponent
 * shuffles. Chapter III is the standard transforming-Saga final chapter
 * ([Effects.ExileAndReturnTransformed], as on The Legend of Roku / Kuruk).
 *
 * The back face reuses [firebending] (the display keyword + attack-triggered "add {R}{R}{R} until end
 * of combat") and [Keyword.MENACE], plus a pay-{X} *targeted* reflexive reanimation. Because the
 * reanimation targets "any number of target creature cards with total mana value X or less"
 * (CR 115.1a / 601.2c), the targets are chosen after X is paid: [Effects.MayPayX] gates on paying X,
 * and its post-payment `then` is a [ReflexiveTriggerEffect] whose [TargetObject] is `unlimited` (any
 * number) with the new [TargetObject.totalAtMost]` = TotalCap.manaValue(DynamicAmount.XValue)` aggregate cap —
 * resolved against the X in scope, and scoped to the damaged player's graveyard via the new
 * `GameObjectFilter.ownedByTriggeringPlayer()`. The chosen cards are then reanimated under your control
 * ([CardDestination.ToZone] battlefield sets the controller to you while ownership stays with the
 * damaged player).
 */
private val FireLordSozin = card("Fire Lord Sozin") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Noble"
    oracleText = "Menace, firebending 3 (Whenever this creature attacks, add {R}{R}{R}. This mana " +
        "lasts until end of combat.)\n" +
        "Whenever Fire Lord Sozin deals combat damage to a player, you may pay {X}. When you do, put " +
        "any number of target creature cards with total mana value X or less from that player's " +
        "graveyard onto the battlefield under your control."
    power = 5
    toughness = 5

    keywords(Keyword.MENACE)
    firebending(3)

    // Whenever Fire Lord Sozin deals combat damage to a player, you may pay {X}. When you do, put
    // any number of target creature cards with total mana value X or less from that player's
    // graveyard onto the battlefield under your control.
    //
    // The reflexive ability is *targeted* (CR 115.1a / 601.2c): once you pay {X}, you announce which
    // creature cards you're reanimating, subject to their combined mana value not exceeding X. We
    // model this by nesting the target selection inside [Effects.MayPayX]'s post-payment `then`, so
    // the `X` just paid is in scope when the reflexive [TargetObject.totalAtMost] cap
    // resolves. The [ReflexiveTriggerEffect] wrapper is borrowed only for its mid-resolution target
    // selection — its `action` is an empty composite because the "when you do" is already the pay-{X}
    // gate. `ownedByTriggeringPlayer()` scopes the graveyard to the player Sozin just damaged.
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.MayPayX(
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                reflexiveTargetRequirements = listOf(
                    TargetObject(
                        unlimited = true,
                        filter = TargetFilter(
                            GameObjectFilter.Creature.ownedByTriggeringPlayer(),
                            zone = Zone.GRAVEYARD
                        ),
                        totalAtMost = TotalCap.manaValue(DynamicAmounts.xValue())
                    )
                ),
                // Put the chosen cards onto the battlefield under your control (owner stays the
                // damaged player).
                reflexiveEffect = Effects.Pipeline {
                    val sozinReanimated = gather(CardSource.ChosenTargets)
                    move(sozinReanimated, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                },
                descriptionOverride = "put any number of target creature cards with total mana " +
                    "value X or less from that player's graveyard onto the battlefield under your control"
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "117"
        artist = "Mitori"
        imageUri = "https://cards.scryfall.io/normal/back/1/4/14eadf46-90c2-4376-8183-6a922a60174d.jpg?1768466342"
    }
}

private val TheRiseOfSozinFront = card("The Rise of Sozin") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Destroy all creatures.\n" +
        "II — Choose a card name. Search target opponent's graveyard, hand, and library for up to " +
        "four cards with that name and exile them. Then that player shuffles.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    // I — Destroy all creatures.
    sagaChapter(1) {
        effect = Effects.DestroyAll(GameObjectFilter.Creature)
    }

    // II — Choose a card name. Search target opponent's graveyard, hand, and library for up to four
    // cards with that name and exile them. Then that player shuffles.
    sagaChapter(2) {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            // Choose a card name.
            val sozinChosenName = chooseCardName(prompt = "Choose a card name")
            // Search the target opponent's graveyard, hand, and library for cards with that name.
            val sozinMatches = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Any.namedFromVariable(sozinChosenName)
                ),
                search = true
            )
            // Up to four of them.
            val sozinToExile = chooseUpTo(
                4,
                from = sozinMatches,
                prompt = "Choose up to four cards to exile",
                selectedLabel = "Exile"
            )
            // Exile them.
            exile(sozinToExile, opponent.asPlayer)
            // Then that player shuffles.
            run(Effects.ShuffleLibrary(target = opponent))
        }
    }

    // III — Exile this Saga, then return it to the battlefield transformed under your control.
    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "117"
        artist = "Mitori"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14eadf46-90c2-4376-8183-6a922a60174d.jpg?1768466342"
    }
}

val TheRiseOfSozin: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TheRiseOfSozinFront,
    backFace = FireLordSozin,
)
