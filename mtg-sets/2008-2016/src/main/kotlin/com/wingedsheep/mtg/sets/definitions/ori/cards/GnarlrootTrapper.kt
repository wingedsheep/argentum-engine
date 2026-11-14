package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gnarlroot Trapper
 * {B}
 * Creature — Elf Druid
 * 1/1
 *
 * {T}, Pay 1 life: Add {G}. Spend this mana only to cast an Elf creature spell.
 * {T}: Target attacking Elf you control gains deathtouch until end of turn.
 *
 * "An Elf creature spell" is [ManaRestriction.SubtypeSpellsOrAbilitiesOnly] with
 * `creatureOnly = true` (the Haven of the Spirit Dragon shape): a noncreature Elf spell (a Kindred
 * Elf instant) or an Elf's activated ability can't spend it. The {G} in the rules text puts green in
 * the color identity, though the card itself is mono-black.
 */
val GnarlrootTrapper = card("Gnarlroot Trapper") {
    manaCost = "{B}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "{T}, Pay 1 life: Add {G}. Spend this mana only to cast an Elf creature spell.\n" +
        "{T}: Target attacking Elf you control gains deathtouch until end of turn. " +
        "(Any amount of damage it deals to a creature is enough to destroy it.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddMana(
            Color.GREEN,
            restriction = ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Elf", creatureOnly = true),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}, Pay 1 life: Add {G}. Spend this mana only to cast an Elf creature spell."
    }

    activatedAbility {
        cost = Costs.Tap
        val elf = target(TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.ELF).attacking().youControl()))
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, elf)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e05572ba-3a26-41f5-96c1-960ec9367178.jpg?1783938341"
    }
}
