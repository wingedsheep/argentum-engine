package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Baleful Beholder
 * {4}{B}{B}
 * Creature — Beholder
 * 6/5
 * When this creature enters, choose one —
 * • Antimagic Cone — Each opponent sacrifices an enchantment of their choice.
 * • Fear Ray — Creatures you control gain menace until end of turn.
 *
 * Antimagic Cone is the untargeted edict over `Player.EachOpponent`; Fear Ray grants menace to the
 * creatures you control as the ability resolves (this creature included).
 */
val BalefulBeholder = card("Baleful Beholder") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Beholder"
    oracleText = "When this creature enters, choose one —\n" +
        "• Antimagic Cone — Each opponent sacrifices an enchantment of their choice.\n" +
        "• Fear Ray — Creatures you control gain menace until end of turn. " +
        "(A creature with menace can't be blocked except by two or more creatures.)"
    power = 6
    toughness = 5
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.Sacrifice(
                    filter = GameObjectFilter.Enchantment,
                    target = EffectTarget.PlayerRef(Player.EachOpponent),
                ),
                "Antimagic Cone — Each opponent sacrifices an enchantment",
            ),
            Mode.noTarget(
                Patterns.Group.grantKeywordToAll(
                    keyword = Keyword.MENACE,
                    filter = GroupFilter.AllCreaturesYouControl,
                ),
                "Fear Ray — Creatures you control gain menace until end of turn",
            ),
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e52d6ef6-d8a3-4288-8917-4c0a179ed4e0.jpg?1783926500"
        ruling("2021-07-23", "In a multiplayer game, each opponent in turn order will choose which enchantment to sacrifice, and they get to know the choices made before them by other players. Once those decisions are made, the chosen enchantments are all sacrificed at the same time.")
    }
}
