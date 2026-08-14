package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.BecomeArtifactEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Supper for Spiders
 * {1}{B}
 * Instant
 *
 * Put onto the battlefield under your control all creature cards in your opponents' graveyards that
 * were put there from the battlefield this turn. They are Food artifacts with "{2}, {T}, Sacrifice
 * this artifact: You gain 3 life." (They lose all other types and subtypes.)
 *
 * Modeling notes — composition only:
 *  - **"…that were put there from the battlefield this turn"** is the existing
 *    `putIntoGraveyardFromBattlefieldThisTurn()` filter predicate (Lobelia Sackville-Baggins), so a
 *    creature card discarded or milled into a graveyard this turn is correctly *not* eligible.
 *  - **Untargeted mass reanimation** — no "target" appears in the text, so the cards are gathered at
 *    resolution: `CardSource.FromZone(GRAVEYARD, Player.EachOpponent, …)` reads every opponent's
 *    graveyard at once, and the move names `Player.You` as the destination controller ("under your
 *    control") while the cards stay owned by their owners.
 *  - **The Food transform** is [BecomeArtifactEffect] per returned card, mirroring Vraska, the
 *    Silencer's Treasure conversion: Layer 4 `SetCardTypes(ARTIFACT)` + `SetAllSubtypes(Food)` is
 *    exactly "they lose all other types and subtypes", and the sac-for-life ability is the durable
 *    `grantedAbility`. Two deliberate departures from Vraska: `colors = null` (the reminder text
 *    mentions only types and subtypes, so the cards keep their own colors) and
 *    `loseAllAbilities = false` (nothing in the text removes abilities — a reanimated Blood Artist
 *    still has its trigger, it just isn't a creature any more). [Duration.Permanent] keeps the
 *    transform in force until the permanent next leaves the battlefield.
 *  - The transform runs *after* the move, once per card, with `EffectTarget.Self` bound to each
 *    iterated entity — the entity id survives the graveyard → battlefield transition, so the
 *    continuous effects land on the permanents that just entered.
 *
 * Edge cases: nothing died from an opponent's battlefield this turn → the gather is empty and the
 * spell resolves with no effect (it still resolves; it has no targets to fizzle on). The returned
 * permanents are artifacts, not creatures, so they have no summoning sickness concern for the
 * `{T}` in the granted ability beyond the ordinary artifact rule — CR 302.6's tap restriction
 * applies to creatures, and these are not.
 */

/** The Food token's printed ability, granted to each card returned this way. */
private val foodSacrificeAbility = ActivatedAbility(
    cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf),
    effect = Effects.GainLife(3),
    descriptionOverride = "{2}, {T}, Sacrifice this artifact: You gain 3 life."
)

val SupperForSpiders = card("Supper for Spiders") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Put onto the battlefield under your control all creature cards in your opponents' " +
        "graveyards that were put there from the battlefield this turn. They are Food artifacts " +
        "with \"{2}, {T}, Sacrifice this artifact: You gain 3 life.\" (They lose all other types " +
        "and subtypes.)"

    spell {
        effect = Effects.Pipeline {
            val fallen = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.EachOpponent,
                    filter = GameObjectFilter.Creature.putIntoGraveyardFromBattlefieldThisTurn()
                )
            )
            move(fallen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(
                ForEachInCollectionEffect(
                    collection = fallen.key,
                    effect = BecomeArtifactEffect(
                        target = EffectTarget.Self,
                        cardTypes = setOf("ARTIFACT"),
                        subtypes = setOf(Subtype.FOOD.value),
                        colors = null,
                        loseAllAbilities = false,
                        grantedAbility = foodSacrificeAbility,
                        duration = Duration.Permanent
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Michele Giorgi"
        flavorText = "The spiders laughed. \"You were quite right,\" they said, \"the meat's alive and kicking!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b25e454-06bb-43ca-9a9f-57164f7a70c4.jpg?1784376962"
    }
}
