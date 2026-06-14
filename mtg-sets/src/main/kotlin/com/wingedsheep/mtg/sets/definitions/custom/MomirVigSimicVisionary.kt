package com.wingedsheep.mtg.sets.definitions.custom

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Momir Vig, Simic Visionary — the Vanguard avatar of the Momir Basic format
 * (<https://mtg.fandom.com/wiki/Momir>).
 *
 * Vanguard (Hand Modifier +0, Life Modifier +4)
 * {X}, Discard a card: Create a token that's a copy of a randomly chosen creature card with
 *   mana value X. Activate only as a sorcery and only once each turn.
 *
 * Lives only in the command zone — never cast, never a permanent (hence the [CardType.VANGUARD]
 * type and no mana cost). [com.wingedsheep.engine.core.GameInitializer] places one copy in each
 * player's command zone when the game's [com.wingedsheep.sdk.core.Format.MomirBasic] is active, and
 * `CommandZoneAbilityEnumerator` surfaces this ability from there.
 *
 * The random copy is drawn from the format's eligible-creature pool; see
 * [com.wingedsheep.sdk.scripting.effects.CreateRandomCreatureTokenWithManaValueEffect].
 */
val MomirVigSimicVisionary = card("Momir Vig, Simic Visionary") {
    typeLine = "Vanguard"
    oracleText = "{X}, Discard a card: Create a token that's a copy of a randomly chosen " +
        "creature card with mana value X. Activate only as a sorcery and only once each turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.DiscardCard)
        effect = Effects.CreateRandomCreatureTokenWithManaValue(DynamicAmount.XValue)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        activateFromZone = Zone.COMMAND
    }

    // Vanguard avatars aren't in any Scryfall set we generate URLs for, so point directly at the
    // official avatar art (used wherever the card renders — command zone and on the stack).
    metadata {
        imageUri = "https://cards.scryfall.io/large/front/3/8/3831df29-83d4-48d6-a3d6-a5fbd5ddf30d.jpg?1562908207"
    }
}
