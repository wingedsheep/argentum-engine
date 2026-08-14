package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Henrika Domnathi // Henrika, Infernal Seer (Innistrad: Crimson Vow)
 * {2}{B}{B}
 * Legendary Creature — Vampire // Legendary Creature — Vampire
 *
 * Front — Henrika Domnathi (1/3)
 *   Flying
 *   At the beginning of combat on your turn, choose one that hasn't been chosen —
 *   • Each player sacrifices a creature of their choice.
 *   • You draw a card and you lose 1 life.
 *   • Transform Henrika.
 *
 * Back — Henrika, Infernal Seer (3/4)
 *   Flying, deathtouch, lifelink
 *   {1}{B}: Each creature you control with flying, deathtouch, and/or lifelink gets +1/+0 until
 *   end of turn.
 *
 * "Choose one that hasn't been chosen" is [ModalEffect.chooseOneNotYetChosen] (Zuko, Conflicted's
 * idiom): the engine remembers modes already chosen by this object and never re-offers them, so once
 * "transform" is picked the modal is gone with the front face. The "each player sacrifices a
 * creature of their choice" mode is [ForEachPlayerEffect] over [ForceSacrificeEffect] with
 * `target = Controller` so each player chooses their own. The back's activated ability pumps every
 * creature you control that has flying, deathtouch, and/or lifelink via [Effects.ForEachInGroup]
 * over a keyword-union [GroupFilter], applying +1/+0 to each ([EffectTarget.Self] inside the group).
 * The back is a transformed face with no mana cost, so its color comes from a color indicator
 * (CR 204): `colorIndicator = "B"`.
 */

private val HenrikaDomnathiFront = card("Henrika Domnathi") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Vampire"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "At the beginning of combat on your turn, choose one that hasn't been chosen —\n" +
        "• Each player sacrifices a creature of their choice.\n" +
        "• You draw a card and you lose 1 life.\n" +
        "• Transform Henrika."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = ModalEffect.chooseOneNotYetChosen(
            // • Each player sacrifices a creature of their choice.
            Mode.noTarget(
                ForEachPlayerEffect(
                    players = Player.Each,
                    effects = listOf(
                        ForceSacrificeEffect(
                            filter = GameObjectFilter.Creature,
                            count = 1,
                            target = EffectTarget.Controller,
                        ),
                    ),
                ),
                "Each player sacrifices a creature of their choice",
            ),
            // • You draw a card and you lose 1 life.
            Mode.noTarget(
                Effects.DrawCards(1).then(Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You))),
                "You draw a card and you lose 1 life",
            ),
            // • Transform Henrika.
            Mode.noTarget(
                TransformEffect(EffectTarget.Self),
                "Transform Henrika",
            ),
        )
        description = "At the beginning of combat on your turn, choose one that hasn't been chosen —"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "119"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/946ca338-5f43-4cff-bd93-1b28449c5fdc.jpg?1783924867"
    }
}

private val HenrikaInfernalSeer = card("Henrika, Infernal Seer") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Creature — Vampire"
    power = 3
    toughness = 4
    oracleText = "Flying, deathtouch, lifelink\n" +
        "{1}{B}: Each creature you control with flying, deathtouch, and/or lifelink gets +1/+0 " +
        "until end of turn."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH, Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter.Creature.youControl().withKeyword(Keyword.FLYING) or
                    GameObjectFilter.Creature.youControl().withKeyword(Keyword.DEATHTOUCH) or
                    GameObjectFilter.Creature.youControl().withKeyword(Keyword.LIFELINK),
            ),
            Effects.ModifyStats(1, 0, EffectTarget.Self),
        )
        description = "Each creature you control with flying, deathtouch, and/or lifelink gets +1/+0 " +
            "until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "119"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/back/9/4/946ca338-5f43-4cff-bd93-1b28449c5fdc.jpg?1783924867"
    }
}

val HenrikaDomnathi: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = HenrikaDomnathiFront,
    backFace = HenrikaInfernalSeer,
)
