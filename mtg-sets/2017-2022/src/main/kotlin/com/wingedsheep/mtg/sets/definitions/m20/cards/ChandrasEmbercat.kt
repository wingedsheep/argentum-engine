package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Chandra's Embercat (Core Set 2020 #129)
 * {1}{R} · Creature — Elemental Cat 2/2
 * {T}: Add {R}. Spend this mana only to cast an Elemental spell or a Chandra planeswalker spell.
 *
 * `AnyOf(Elemental spell, AllOf(planeswalker spell, Chandra spell))`: a "Chandra planeswalker
 * spell" is a planeswalker spell with the subtype Chandra, so both atoms of the second branch
 * must hold — an Ajani planeswalker or a Chandra-named non-planeswalker can't spend it.
 */
val ChandrasEmbercat = card("Chandra's Embercat") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Cat"
    power = 2
    toughness = 2
    oracleText = "{T}: Add {R}. Spend this mana only to cast an Elemental spell or a Chandra planeswalker spell."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.RED,
            restriction = ManaRestriction.AnyOf(
                listOf(
                    ManaRestriction.SubtypeSpellsOnly(setOf("Elemental")),
                    ManaRestriction.AllOf(
                        listOf(
                            ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.PLANESWALKER),
                            ManaRestriction.SubtypeSpellsOnly(setOf("Chandra"))
                        )
                    )
                )
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Johan Grenier"
        flavorText = "\"Furballs? Try dealing with fireballs.\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30f9c266-f32e-4483-9709-f83675331688.jpg?1783932982"
        ruling(
            "2019-07-12",
            "A Chandra planeswalker spell is a planeswalker spell with the subtype Chandra. Cards that use Chandra's name in their title or her likeness in their illustration (such as Chandra's Regulator or Chandra's Outrage) aren't Chandra planeswalker spells."
        )
    }
}
