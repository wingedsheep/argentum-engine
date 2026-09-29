package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glissa, Herald of Predation — March of the Machine #226
 * {3}{B}{G} · Legendary Creature — Phyrexian Zombie Elf 3/5
 *
 * A modal beginning-of-combat trigger. "Incubate 2 twice" is two separate incubates (two tokens);
 * "transform all Incubator tokens you control" walks the group like The Argent Etchings; the third
 * mode grants both keywords in one pass over the Phyrexians, read as the mode resolves.
 */
val GlissaHeraldOfPredation = card("Glissa, Herald of Predation") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Phyrexian Zombie Elf"
    power = 3
    toughness = 5
    oracleText = "At the beginning of combat on your turn, choose one —\n" +
        "• Incubate 2 twice. (To incubate 2, create an Incubator token with two +1/+1 counters on it " +
        "and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)\n" +
        "• Transform all Incubator tokens you control.\n" +
        "• Phyrexians you control gain first strike and deathtouch until end of turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = ModalEffect.chooseOne(
            mode("Incubate 2 twice.") {
                effect = Patterns.Mechanic.incubate(2) then Patterns.Mechanic.incubate(2)
            },
            mode("Transform all Incubator tokens you control.") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Permanent.withSubtype("Incubator").token().youControl()),
                    Effects.Transform(EffectTarget.IterationEntity)
                )
            },
            mode("Phyrexians you control gain first strike and deathtouch until end of turn.") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.PHYREXIAN).youControl()),
                    Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.IterationEntity) then
                        Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.IterationEntity)
                )
            }
        )
        description = "At the beginning of combat on your turn, choose one — Incubate 2 twice; or " +
            "transform all Incubator tokens you control; or Phyrexians you control gain first strike " +
            "and deathtouch until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c299066-dfdd-47a3-85e6-225508ba95fe.jpg?1783916954"
    }
}
