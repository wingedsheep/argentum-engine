package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Selfless Glyphweaver // Deadly Vanity — Strixhaven: School of Mages #157 (canonical printing)
 * Modal double-faced card (CR 712). Cast either face from hand, never both.
 *
 * Front — Selfless Glyphweaver ({2}{W}, Creature — Human Cleric, 2/3):
 *   "Exile this creature: Creatures you control gain indestructible until end of turn."
 *   Selfless Spirit's ability with an exile-self cost: the creatures are locked in as the
 *   ability resolves, so later arrivals don't gain indestructible (ruling).
 *
 * Back — Deadly Vanity ({5}{B}{B}{B}, Sorcery):
 *   "Choose a creature or planeswalker, then destroy all other creatures and planeswalkers."
 *   Duneblast's non-targeting save-one pipeline over creatures and planeswalkers, with exactly one
 *   choice — you must choose one if any exists, and may pick a hexproof or opposing one
 *   (rulings). The rest are destroyed at once, with no window to respond after the choice.
 */
val SelflessGlyphweaver = card("Selfless Glyphweaver") {
    manaCost = "{2}{W}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Cleric"
    oracleText = "Exile this creature: Creatures you control gain indestructible until end of turn."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.ExileSelf
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
        )
        description = "Exile this creature: Creatures you control gain indestructible until end of turn."
    }

    modalBack("Deadly Vanity") {
        manaCost = "{5}{B}{B}{B}"
        typeLine = "Sorcery"
        oracleText = "Choose a creature or planeswalker, then destroy all other creatures and planeswalkers."
        imageUri = "https://cards.scryfall.io/normal/back/8/9/8982ff88-8595-4363-8cde-6e87fb57a2d8.jpg?1783927331"
        spell {
            effect = Effects.Pipeline {
                val all = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.CreatureOrPlaneswalker))
                val (_, toDestroy) = chooseExactlySplit(
                    1,
                    from = all,
                    prompt = "Choose a creature or planeswalker to save",
                    selectedLabel = "Save",
                    remainderLabel = "Destroy",
                    useTargetingUI = true
                )
                destroy(toDestroy)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Johannes Voss"
        flavorText = "He offered his light to make sure no others would be dimmed."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8982ff88-8595-4363-8cde-6e87fb57a2d8.jpg?1783927331"
        ruling("2021-04-16", "You may activate Selfless Glyphweaver's ability only if Selfless Glyphweaver is on the battlefield. Only creatures you control as that ability resolves will gain indestructible. Creatures that come under your control later in the turn will not.")
        ruling("2021-04-16", "As Deadly Vanity resolves, you choose which creature or planeswalker you are saving. Once the choice is made, there is no window for players to respond before the rest of the creatures and planeswalkers are destroyed.")
        ruling("2021-04-16", "You may choose any creature or planeswalker, even one controlled by an opponent.")
        ruling("2021-04-16", "You must choose a creature or planeswalker if there is one on the battlefield.")
        ruling("2021-04-16", "Because Deadly Vanity does not target the permanent you choose to save, you may choose creatures with hexproof or protection from black.")
    }
}
