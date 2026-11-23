package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ash Zealot
 * {R}{R}
 * Creature — Human Warrior
 * 2/2
 * First strike, haste
 * Whenever a player casts a spell from a graveyard, this creature deals 3 damage to that player.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing. Current Oracle text
 * (errata) adds haste, which the original printing lacked.
 *
 * River Kelpie's "casts a spell from a graveyard" trigger (any player, any graveyard) combined with
 * Eidolon of the Great Revel's damage to the caster ([Player.TriggeringPlayer]), not a target.
 */
val AshZealot = card("Ash Zealot") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "First strike, haste\n" +
        "Whenever a player casts a spell from a graveyard, this creature deals 3 damage to that player."

    keywords(Keyword.FIRST_STRIKE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(requires = setOf(SpellCastPredicate.CastFromZone(Zone.GRAVEYARD)))
        effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever a player casts a spell from a graveyard, this creature deals 3 damage to that player."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Eric Deschamps"
        flavorText = "\"Smarter than death? Let's see you outsmart my mace, necromancer!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f61b1a3-4b3b-4490-a9dc-17aac258cbda.jpg?1783940358"
        ruling(
            "2012-10-01",
            "Ash Zealot's ability doesn't allow any player to cast cards from a graveyard. Such cards need " +
                "another way to be cast, such as the flashback ability."
        )
        ruling(
            "2012-10-01",
            "Ash Zealot's triggered ability will resolve before that spell resolves."
        )
        ruling(
            "2012-10-01",
            "Ash Zealot's ability doesn't trigger if you put a card onto the battlefield from a graveyard " +
                "(using Grave Betrayal's ability, for example)."
        )
    }
}
