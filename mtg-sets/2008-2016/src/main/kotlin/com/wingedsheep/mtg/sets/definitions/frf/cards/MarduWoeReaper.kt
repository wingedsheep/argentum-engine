package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mardu Woe-Reaper
 * {W}
 * Creature — Human Warrior
 * 2/1
 *
 * Whenever this creature or another Warrior you control enters, you may exile target creature
 * card from a graveyard. If you do, you gain 1 life.
 *
 * "This creature or another Warrior you control" is the corpus's `Triggers.a(Warrior you control)`
 * spelling (the Ally cycle, Nebelgast Herald): the source is itself a Warrior, so it sees its own
 * entry. The life gain sits behind the same "may" as the exile, so declining gains nothing, and
 * the exile is the ability's only target — if that card has left the graveyard by resolution the
 * whole ability fizzles (CR 608.2b) and no life is gained either.
 */
val MarduWoeReaper = card("Mardu Woe-Reaper") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    oracleText = "Whenever this creature or another Warrior you control enters, you may exile target " +
        "creature card from a graveyard. If you do, you gain 1 life."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.WARRIOR).youControl()).enters()
        val exiled = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.May(Effects.Exile(exiled) then Effects.GainLife(1))
        description = "Whenever this creature or another Warrior you control enters, you may exile " +
            "target creature card from a graveyard. If you do, you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Willian Murai"
        flavorText = "\"Those who died in battle deserve their rest.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bf14a32-b97b-48c7-8395-d9c3626735c1.jpg?1783938711"
    }
}
