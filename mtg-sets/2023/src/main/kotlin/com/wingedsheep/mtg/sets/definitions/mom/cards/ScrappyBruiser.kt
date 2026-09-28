package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scrappy Bruiser
 * {3}{R}
 * Creature — Raccoon Warrior
 * 3/4
 * Whenever this creature attacks, up to one target attacking creature gets +2/+0 and gains trample until
 * end of turn. Return that creature to its owner's hand at end of combat.
 */
val ScrappyBruiser = card("Scrappy Bruiser") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Raccoon Warrior"
    oracleText = "Whenever this creature attacks, up to one target attacking creature gets +2/+0 and gains " +
        "trample until end of turn. Return that creature to its owner's hand at end of combat. (Return it only " +
        "if it's on the battlefield.)"
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.AttackingCreature, optional = true)
        effect = Effects.ModifyStats(2, 0, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
            Effects.CreateDelayedTrigger(
                step = Step.END_COMBAT,
                effect = Effects.ReturnToHand(creature),
            )
        description = "Whenever this creature attacks, up to one target attacking creature gets +2/+0 and " +
            "gains trample until end of turn. Return that creature to its owner's hand at end of combat."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "David Auden Nash"
        flavorText = "\"My flesh is an affront to existence? Come say that to my fist, you bloated rust bucket.\"\n—Koko, Riveteers welder"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab6c877a-25f7-4ee7-8cf0-33728ef085fc.jpg?1783916982"
    }
}
