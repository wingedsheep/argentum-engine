package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Desolation of Smaug — The Hobbit #93
 * {2}{R}{R} · Sorcery · Rare
 *
 * Desolation of Smaug deals 3 damage to each non-Dragon creature.
 * Add four mana in any combination of colors. Spend this mana only to cast Dragon spells.
 *
 * Modeling notes:
 *  - The sweep is the Slagstorm / Breath of Darigaaz idiom: `ForEachInGroup` over a
 *    projection-read battlefield group with a per-creature [DealDamageEffect]. `notSubtype(Dragon)`
 *    rather than a hand-rolled predicate, so a creature that has *become* a Dragon (Beorn the
 *    Fierce's Bear-making cousin, a Shapeshifter) is correctly spared.
 *  - The mana rider is not a mana ability — it's part of a sorcery's resolution, so the four mana
 *    land in the controller's pool mid-resolution and empty at end of step like any other mana
 *    (CR 500.4). In practice that means casting the Dragon in the same main phase, right after this
 *    resolves.
 *  - "In any combination of colors" is [Effects.AddManaInAnyCombination], not `AddAnyColorMana`:
 *    each of the four pips is colored independently, so {R}{R}{G}{U} is a legal split.
 *  - The spend restriction is [ManaRestriction.SubtypeSpellsOnly] on "Dragon" — spells only, which
 *    matches the text ("to cast Dragon spells"); it can't pay for a Dragon's activated ability.
 */
val DesolationOfSmaug = card("Desolation of Smaug") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Desolation of Smaug deals 3 damage to each non-Dragon creature.\n" +
        "Add four mana in any combination of colors. Spend this mana only to cast Dragon spells."

    spell {
        effect = Effects.Composite(
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.DRAGON)),
                DealDamageEffect(3, EffectTarget.Self)
            ),
            Effects.AddManaInAnyCombination(
                amount = 4,
                restriction = ManaRestriction.SubtypeSpellsOnly(setOf("Dragon"))
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Irvin Rodriguez"
        flavorText = "At the twanging of the bows and the shrilling of the trumpets, the Dragon's " +
            "wrath blazed to its height, till he was blind and mad with it."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2462358b-52c8-49b2-8d97-d65a9188f8f7.jpg?1784376974"
    }
}
