package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Toolcraft Exemplar
 * {W}
 * Creature — Dwarf Artificer
 * 1/1
 * At the beginning of combat on your turn, if you control an artifact, this creature gets +2/+1
 * until end of turn. If you control three or more artifacts, it also gains first strike until
 * end of turn.
 */
val ToolcraftExemplar = card("Toolcraft Exemplar") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Artificer"
    oracleText = "At the beginning of combat on your turn, if you control an artifact, this creature " +
        "gets +2/+1 until end of turn. If you control three or more artifacts, it also gains first " +
        "strike until end of turn."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouControl(GameObjectFilter.Artifact)
        // The three-artifact check happens only as the ability resolves (2016-09-20 ruling).
        effect = Effects.ModifyStats(2, 1, EffectTarget.Self) then
            Effects.If(
                Conditions.YouControlAtLeast(3, GameObjectFilter.Artifact),
                Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Zezhou Chen"
        flavorText = "\"If you wish to produce the finest work, you must start with the finest tools.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daae3dae-3297-4750-9bca-c8aadaf815ea.jpg?1783937228"
        ruling(
            "2016-09-20",
            "If you control no artifacts as your combat phase begins, Toolcraft Exemplar's triggered ability " +
                "doesn't trigger at all. If you control no artifacts as the triggered ability resolves, it has no effect."
        )
        ruling(
            "2016-09-20",
            "Whether or not you control three or more artifacts is checked only as the triggered ability resolves."
        )
    }
}
