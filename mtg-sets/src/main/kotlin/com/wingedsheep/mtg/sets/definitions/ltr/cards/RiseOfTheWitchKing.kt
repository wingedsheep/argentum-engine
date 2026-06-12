package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rise of the Witch-king
 * {2}{B}{G}
 * Sorcery
 *
 * Each player sacrifices a creature of their choice. If you sacrificed a creature this
 * way, you may return another permanent card from your graveyard to the battlefield.
 *
 * The oracle text does not say "target" — the reanimation is a *resolution-time* choice,
 * not a cast-time target. The card has no targets, so it can resolve even if all eligible
 * graveyard cards become invalid between cast and resolution.
 *
 * Composition:
 *  - `Effects.Sacrifice(Creature, count=1, target=Player.Each)` — each player auto-sacrifices
 *    a sole creature or chooses among multiples. Snapshots flow into
 *    `EffectContext.sacrificedPermanents` so the rider can read them.
 *  - The rider is a `ConditionalEffect` gated on `YouSacrificedThisWay` (LTR Gap 17).
 *  - The reanimation half is the standard Gather → Select(`ChooseUpTo(1)`) → Move
 *    pipeline against the graveyard: the player is offered the eligible permanent cards
 *    in their graveyard and may pick zero or one of them. The Gather uses
 *    `excludeSacrificedThisWay = true` so the creature you just sacrificed (now in your
 *    graveyard) is not a legal "another permanent card" choice — per the CR ruling
 *    "You cannot return the same permanent card that you sacrificed."
 */
val RiseOfTheWitchKing = card("Rise of the Witch-king") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Each player sacrifices a creature of their choice. " +
        "If you sacrificed a creature this way, you may return another permanent card " +
        "from your graveyard to the battlefield."

    spell {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            count = 1,
            target = EffectTarget.PlayerRef(Player.Each)
        ).then(
            ConditionalEffect(
                condition = Conditions.YouSacrificedThisWay,
                effect = Effects.Pipeline {
                    val eligible = gather(
                        CardSource.FromZone(
                            Zone.GRAVEYARD,
                            Player.You,
                            GameObjectFilter.Permanent,
                            // "return ANOTHER permanent card" — the creature you just
                            // sacrificed to this spell sits in your graveyard but is not
                            // a legal choice (CR ruling: "You cannot return the same
                            // permanent card that you sacrificed.").
                            excludeSacrificedThisWay = true
                        ),
                        name = "eligible"
                    )
                    val chosen = chooseUpTo(
                        1,
                        from = eligible,
                        prompt = "Choose a permanent card to return to the battlefield",
                        name = "chosen"
                    )
                    move(
                        chosen,
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                    )
                }
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Andrea Piparo"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f440fa1-5387-41d6-a80f-5b19dbb21514.jpg?1686969962"
    }
}
