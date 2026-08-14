package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mysterio, Master of Illusion
 * {3}{U}
 * Legendary Creature — Human Villain
 * 3/3
 *
 * When Mysterio enters, create a 3/3 blue Illusion Villain creature token for each nontoken
 * Villain you control. Exile those tokens when Mysterio leaves the battlefield.
 *
 * Implementation — linked created-token tracking (the Tetravus provenance mechanism):
 *  - ETB: [CreateTokenEffect] with `stampCreator = true` mints one 3/3 blue Illusion Villain per
 *    nontoken Villain you control (Mysterio itself counts — it is a nontoken Villain already on the
 *    battlefield when its own ETB resolves, so the count is always ≥ 1). Each token is stamped with
 *    Mysterio's entity id (`CreatedByComponent`).
 *  - Leaves: a `LeavesBattlefield(SELF)` trigger gathers exactly the permanents whose stamped creator
 *    is this Mysterio (`GameObjectFilter.Any.createdBySource()`, on any battlefield so a control change
 *    doesn't drop them) and exiles them — "those tokens", not "your Illusions". The provenance filter
 *    resolves against the trigger's source id (last-known Mysterio), so it still matches after Mysterio
 *    has left, and only tokens minted by *this* Mysterio object are exiled.
 */
val MysterioMasterOfIllusion = card("Mysterio, Master of Illusion") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Villain"
    power = 3
    toughness = 3
    oracleText = "When Mysterio enters, create a 3/3 blue Illusion Villain creature token for each " +
        "nontoken Villain you control. Exile those tokens when Mysterio leaves the battlefield."

    // ETB: one 3/3 blue Illusion Villain token per nontoken Villain you control.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            count = DynamicAmount.Count(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Creature.withSubtype(Subtype.VILLAIN).youControl().nontoken(),
            ),
            power = 3,
            toughness = 3,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Illusion", "Villain"),
            name = "Illusion Villain",
            imageUri = "https://cards.scryfall.io/normal/front/f/6/f67d6b6d-c57e-431f-ae80-a9658d628827.jpg?1783905184",
            stampCreator = true,
        )
    }

    // Leaves: exile exactly the tokens this Mysterio created.
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.Pipeline {
            val tokens = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Any.createdBySource(),
                    player = Player.Each,
                ),
                name = "mysterioTokens",
            )
            exile(tokens)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Alexander Gering"
        flavorText = "A skilled showman, Quentin Beck incorporated lights, special effects, holography . . . " +
            "all in the service of a . . . DRAMATIC . . . ENTRANCE!"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/facbd96f-c088-4377-b740-5e0fe99102bb.jpg?1783905353"
    }
}
