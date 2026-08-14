package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Web-Shooters
 * {1}{W}
 * Artifact — Equipment
 * Equipped creature gets +1/+1 and has reach and "Whenever this creature attacks,
 * tap target creature an opponent controls."
 * Equip {2}
 */
val WebShooters = card("Web-Shooters") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 and has reach and \"Whenever this creature attacks, tap target creature an opponent controls.\"\nEquip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.Attacks.event,
                binding = Triggers.Attacks.binding,
                effect = Effects.Tap(EffectTarget.ContextTarget(0)),
                targetRequirement = Targets.CreatureOpponentControls,
            ),
            filter = Filters.EquippedCreature,
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Javier Charro"
        flavorText = "\"A spider needs a web! This little device should just do the trick. " +
            "I'll fasten one to each arm—it'll operate by the slightest pressure of any finger.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0ca1108-6d99-4dc2-96ab-7728d65b0c06.jpg?1758203916"
    }
}
