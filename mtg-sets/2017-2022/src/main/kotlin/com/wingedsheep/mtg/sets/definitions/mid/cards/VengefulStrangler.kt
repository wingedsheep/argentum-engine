package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Vengeful Strangler // Strangling Grasp (Innistrad: Midnight Hunt #126)
 * {1}{B} · Creature — Human Rogue 2/1 // Enchantment — Aura
 *
 * Front — Vengeful Strangler
 *   This creature can't block.
 *   When this creature dies, return it to the battlefield transformed under your control attached
 *   to target creature or planeswalker an opponent controls.
 *
 * Back — Strangling Grasp
 *   Enchant creature or planeswalker an opponent controls
 *   At the beginning of your upkeep, enchanted permanent's controller sacrifices a nonland
 *   permanent of their choice, then that player loses 1 life.
 *
 * The return names its controller outright (`controllerOverride = EffectTarget.Controller`):
 * without it, an Aura returned onto a *permanent* follows that permanent's controller, which would
 * hand Strangling Grasp to the opponent it is strangling. "Enchanted permanent's controller" is
 * the controller of the one permanent the Aura is attached to, read through a `ForEachInGroup`
 * over `attachedToBySource()` (one-shot group scans read predicates, not `Scope.AttachedTo`), so
 * a stolen host makes its new controller pay.
 */
private val VengefulStranglerFront = card("Vengeful Strangler") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "This creature can't block.\n" +
        "When this creature dies, return it to the battlefield transformed under your control " +
        "attached to target creature or planeswalker an opponent controls."

    staticAbility {
        ability = CantBlock(filter = GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val host = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.ReturnSelfToBattlefieldAttached(
            target = host,
            transformed = true,
            controllerOverride = EffectTarget.Controller,
        )
        description = "When this creature dies, return it to the battlefield transformed under " +
            "your control attached to target creature or planeswalker an opponent controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Sean Sevestre"
        flavorText = "As the flames consumed the killer, his eyes remained fixed on the " +
            "executioner's face, his hands twitching like angry spiders."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4054ae6-0227-4d99-8cb5-72e8b5d0b726.jpg?1783925614"
    }
}

private val StranglingGrasp = card("Strangling Grasp") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature or planeswalker an opponent controls\n" +
        "At the beginning of your upkeep, enchanted permanent's controller sacrifices a nonland " +
        "permanent of their choice, then that player loses 1 life."

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val victim = EffectTarget.PlayerRef(Player.ControllerOfIterationEntity)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Permanent.attachedToBySource()),
            effect = Effects.Sacrifice(GameObjectFilter.NonlandPermanent, 1, victim) then
                Effects.LoseLife(1, victim),
        )
        description = "At the beginning of your upkeep, enchanted permanent's controller " +
            "sacrifices a nonland permanent of their choice, then that player loses 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Sean Sevestre"
        flavorText = "The next day, the executioner was found with ashen handprints on his throat."
        imageUri = "https://cards.scryfall.io/normal/back/d/4/d4054ae6-0227-4d99-8cb5-72e8b5d0b726.jpg?1783925614"
    }
}

val VengefulStrangler: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = VengefulStranglerFront,
    backFace = StranglingGrasp,
)
