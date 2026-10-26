package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Haven of the Spirit Dragon
 * Land
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast a Dragon creature spell.
 * {2}, {T}, Sacrifice this land: Return target Dragon creature card or Ugin planeswalker card from
 * your graveyard to your hand.
 *
 * The restricted mana is [ManaRestriction.SubtypeSpellsOrAbilitiesOnly] with `creatureOnly = true`
 * — creature spells with the Dragon subtype only, never abilities (Rivaz of the Claw's shape). It
 * covers additional and alternative costs of such a spell, dash included (2015-02-25 ruling).
 *
 * The return target is a homogeneous OR of the two card kinds, both in your graveyard.
 */
val HavenOfTheSpiritDragon = card("Haven of the Spirit Dragon") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a Dragon creature spell.\n" +
        "{2}, {T}, Sacrifice this land: Return target Dragon creature card or Ugin planeswalker card " +
        "from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(
            restriction = ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Dragon", creatureOnly = true),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of any color. Spend this mana only to cast a Dragon creature spell."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        val card = target(
            TargetFilter(
                (GameObjectFilter.Creature.withSubtype(Subtype.DRAGON) or
                    GameObjectFilter.Planeswalker.withSubtype("Ugin")).ownedByYou(),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.ReturnToHand(card)
        description = "{2}, {T}, Sacrifice this land: Return target Dragon creature card or Ugin " +
            "planeswalker card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9910042e-091f-47da-ad29-8f76c9d3b8c1.jpg?1783938567"
        ruling("2015-02-25", "The mana produced by Haven of the Spirit Dragon's second ability can be used to pay for any additional or alternative costs (such as dash costs) involved in casting a Dragon creature spell.")
    }
}
