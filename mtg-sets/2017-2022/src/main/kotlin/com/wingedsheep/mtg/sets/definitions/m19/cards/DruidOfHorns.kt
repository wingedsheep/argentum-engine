package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Druid of Horns — Core Set 2019 #176 (canonical printing)
 * {3}{G}
 * Creature — Human Druid
 * 2/3
 * Whenever you cast an Aura spell that targets this creature, create a 3/3 green Beast creature token.
 *
 * The heroic-style cast trigger (`TargetsSource`) narrowed to Aura spells — an Aura's enchant
 * target is the spell's cast-time target, so it is what `TargetsSource` reads.
 */
val DruidOfHorns = card("Druid of Horns") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast an Aura spell that targets this creature, create a 3/3 green Beast creature token."

    triggeredAbility {
        trigger = Triggers.you.casts(
            spell = GameObjectFilter.Enchantment.withSubtype(Subtype.AURA),
            requires = setOf(SpellCastPredicate.TargetsSource),
        )
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
            imageUri = "https://cards.scryfall.io/normal/front/8/3/832691a5-c638-481b-a413-3fdfc604a132.jpg?1783934466",
        )
        description = "Whenever you cast an Aura spell that targets this creature, create a 3/3 green Beast creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Nils Hamm"
        flavorText = "No welcome for those who trespass. Only fang and horn and claw."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19ca91ea-dffd-44a9-a54a-c664d83c357b.jpg?1783934537"
        ruling("2018-07-13", "Druid of Horns's ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
