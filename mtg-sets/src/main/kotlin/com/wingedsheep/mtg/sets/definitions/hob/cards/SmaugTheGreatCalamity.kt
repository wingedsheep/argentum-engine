package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Smaug, the Great Calamity // Spew Flame — The Hobbit #109
 * {5}{R}{R} · Legendary Creature — Dragon · Common
 * 5/5
 *
 * Flying
 *
 * Adventure: Spew Flame — {4}{R}, Sorcery — Adventure
 * Spew Flame deals 5 damage to target creature.
 *
 * A vanilla flier on the front and a plain burn spell on the Adventure, so both halves are pure
 * facade composition. The damage is dealt by the Adventure card itself (`EffectTarget.Source` is
 * implied by [Effects.DealDamage]'s default source), which matters only for damage-source-triggered
 * abilities on the target.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val SmaugTheGreatCalamity = card("Smaug, the Great Calamity") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    adventure("Spew Flame") {
        manaCost = "{4}{R}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Spew Flame deals 5 damage to target creature. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            target = Targets.Creature
            effect = Effects.DealDamage(5, EffectTarget.ContextTarget(0))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Chris Cold"
        flavorText = "\"My armor is like tenfold shields, my teeth are swords, my wings a hurricane, " +
            "and my breath death!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/1/419ca9e5-8413-4378-a4ef-eda5a1024218.jpg?1785497136"
    }
}
