package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oscorp Industries — Marvel's Spider-Man #182
 * Land
 *
 * This land enters tapped.
 * When this land enters from a graveyard, you lose 2 life.
 * {T}: Add {U}, {B}, or {R}.
 * Mayhem (You may play this card from your graveyard if you discarded it this turn. Timing rules
 * still apply.)
 *
 * The Mayhem here is the CR 702.187c no-cost land form (`mayhem("")`): a discarded-this-turn
 * Oscorp is *played* from the graveyard (enumerated by `PlayLandEnumerator`, allowed by
 * `PlayLandHandler`), which trips the "enters from a graveyard" ETB via the
 * `EnteredFromGraveyardComponent` the handler now stamps on graveyard land-plays.
 */
val OscorpIndustries = card("Oscorp Industries") {
    typeLine = "Land"
    colorIdentity = "UBR"
    oracleText = "This land enters tapped.\n" +
        "When this land enters from a graveyard, you lose 2 life.\n" +
        "{T}: Add {U}, {B}, or {R}.\n" +
        "Mayhem (You may play this card from your graveyard if you discarded it this turn. " +
        "Timing rules still apply.)"

    replacementEffect(EntersTapped())

    // When this land enters from a graveyard, you lose 2 life. The graveyard check runs at
    // resolution (a ConditionalEffect), since TriggeringEntityEnteredOrWasCastFromGraveyard reads
    // the EnteredFromGraveyardComponent only in resolution context.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ConditionalEffect(
            condition = Conditions.TriggeringEntityEnteredOrWasCastFromGraveyard,
            effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.You))
        )
    }

    // {T}: Add {U}, {B}, or {R}. (Modelled as one mana ability per color, like other trilands.)
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // Mayhem — CR 702.187c no-cost land form.
    mayhem("")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Bastien Grivet"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e609d6e-9e37-45d2-87de-8c76675f7cec.jpg?1783905299"
    }
}
