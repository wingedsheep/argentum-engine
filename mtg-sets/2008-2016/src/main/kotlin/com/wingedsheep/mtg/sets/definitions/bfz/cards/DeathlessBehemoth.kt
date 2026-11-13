package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deathless Behemoth
 * {6}
 * Creature — Eldrazi
 * 6/6
 * Vigilance
 * Sacrifice two Eldrazi Scions: Return this card from your graveyard to your hand.
 * Activate only as a sorcery.
 *
 * The recursion is a graveyard-activated ability (Gilded Assault Cart's shape) at sorcery speed
 * (Deadbridge Goliath's scavenge timing). "Eldrazi Scion" names two creature types at once, so the
 * sacrifice filter requires both subtypes (Drowner of Hope's shape); any Eldrazi Scions you control
 * pay it.
 */
val DeathlessBehemoth = card("Deathless Behemoth") {
    manaCost = "{6}"
    typeLine = "Creature — Eldrazi"
    power = 6
    toughness = 6
    oracleText = "Vigilance\n" +
        "Sacrifice two Eldrazi Scions: Return this card from your graveyard to your hand. " +
        "Activate only as a sorcery."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.SacrificeMultiple(
            2,
            GameObjectFilter.Permanent.withSubtype(Subtype.ELDRAZI).withSubtype("Scion")
        )
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
        description = "Return Deathless Behemoth from your graveyard to your hand"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Jason Felix"
        flavorText = "\"Over and over it rose. Over and over we fell.\"\n—Ganda, Goma Fada deserter"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d98f1df-f99a-43c5-81cd-9d333226857a.jpg?1783938224"
    }
}
