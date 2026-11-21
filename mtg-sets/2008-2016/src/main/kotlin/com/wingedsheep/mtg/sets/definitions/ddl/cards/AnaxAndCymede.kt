package com.wingedsheep.mtg.sets.definitions.ddl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Anax and Cymede — Duel Decks: Heroes vs. Monsters #11 (canonical printing; reprinted in Theros)
 * {1}{R}{W} · Legendary Creature — Human Soldier · 3/2
 *
 * First strike, vigilance
 * Heroic — Whenever you cast a spell that targets Anax and Cymede, creatures you control get +1/+1
 * and gain trample until end of turn.
 *
 * The group is snapshotted as the trigger resolves, so creatures that come under your control later
 * in the turn don't get the bonus (per the ruling).
 */
val AnaxAndCymede = card("Anax and Cymede") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "First strike, vigilance\nHeroic — Whenever you cast a spell that targets Anax and Cymede, creatures you control get +1/+1 and gain trample until end of turn."

    keywords(Keyword.FIRST_STRIKE, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Patterns.Group.pumpAndGrantToAll(
            power = 1,
            toughness = 1,
            keyword = Keyword.TRAMPLE,
            filter = Filters.Group.creaturesYouControl
        )
        description = "Heroic — Whenever you cast a spell that targets Anax and Cymede, creatures you control get +1/+1 and gain trample until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Willian Murai"
        flavorText = "Akros's greatest heroes are also its royalty."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70e5d609-c15e-4da1-b253-abd98e8bdfad.jpg?1783939863"
        ruling("2013-09-15", "Only creatures you control when the heroic ability resolves will get the bonuses. Creatures that come under your control later in the turn will not.")
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
