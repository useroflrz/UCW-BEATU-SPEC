
INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('12536984', '黑璇BLACK', 'https://i2.hdslb.com/bfs/face/05cec9b52c9aeb4ce214c26a5486cbb8a06259c6.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  100,  -- ✅ 修改：BV1CXSbBVEFQ -> 100
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E3%80%90%E7%96%AF%E7%8B%82%E5%8A%A8%E7%89%A9%E5%9F%8E2%E3%80%91%E5%8F%AA%E6%98%AF%E8%A7%86%E8%A7%92%E9%97%AE%E9%A2%98.mp4',
  'http://i1.hdslb.com/bfs/archive/2dc05227c20ecd33264751f82a120c8657e5435c.jpg',
  '【疯狂动物城2】只是视角问题',
  '[]',
  50000,
  'LANDSCAPE',
  '12536984',
  '黑璇BLACK',
  'https://i2.hdslb.com/bfs/face/05cec9b52c9aeb4ce214c26a5486cbb8a06259c6.jpg',
  40756, 447, 11254, 1964, 251956,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  100,  -- ✅ 修改：BV1CXSbBVEFQ -> 100
  '512407709',
  '好名字都被猫取了喵',
  'https://i2.hdslb.com/bfs/face/5aad76e98b8ada49d59d93ee1774a28b5d12dff4.jpg',
  '天哪每个品质对应的角色都好适配，好厉害',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  624,
  FROM_UNIXTIME(1764757824)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  100,  -- ✅ 修改：BV1CXSbBVEFQ -> 100
  '649342997',
  '晓春想摆烂',
  'https://i2.hdslb.com/bfs/face/4ee3680a1bb4571e94028015347077bf36978c41.jpg',
  '萌得我埋头做了十道高数题[大哭]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  4148,
  FROM_UNIXTIME(1764770848)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  100,  -- ✅ 修改：BV1CXSbBVEFQ -> 100
  '3493076209240846',
  '雁司绛',
  'https://i1.hdslb.com/bfs/face/8b9cd257c86ca5f216330ecb5571f57a6d52f2f3.jpg',
  '好萌……看得我哈特软软……',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  420,
  FROM_UNIXTIME(1764740503)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('14804670', '无穷小亮的科普日常', 'http://i0.hdslb.com/bfs/face/6de12181ed59518fc7beff2046fb3218d50206cc.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  101,  -- ✅ 修改：BV1FQUrBzE2P -> 101
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%BD%91%E7%BB%9C%E7%83%AD%E4%BC%A0%E7%94%9F%E7%89%A9%E9%89%B4%E5%AE%9A%20%E7%AC%AC58%E6%9C%9F.mp4',
  'http://i0.hdslb.com/bfs/archive/2df5d0de8cdd197ce99c744a00863bedc8e4a781.jpg',
  '网络热传生物鉴定 第58期',
  '[]',
  699000,
  'PORTRAIT',
  '14804670',
  '无穷小亮的科普日常',
  'http://i0.hdslb.com/bfs/face/6de12181ed59518fc7beff2046fb3218d50206cc.jpg',
  436349, 12962, 29993, 8337, 4997211,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  101,  -- ✅ 修改：BV1FQUrBzE2P -> 101
  '454507',
  '魔幻组曲棱镜娜娜',
  'https://i1.hdslb.com/bfs/face/1907f22d45ea501b4006c09455a8699911dd50fc.jpg',
  '估计未来小亮的鉴定视频，
有一半素材都是AI生成的假动物。[笑哭]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  686,
  FROM_UNIXTIME(1763975663)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  101,  -- ✅ 修改：BV1FQUrBzE2P -> 101
  '124668043',
  '杜烁',
  'https://i1.hdslb.com/bfs/face/0d9da902fc7db5fee44d30eaa249e8e6a35939a8.jpg',
  '04:37 老一辈儿还有说冬天东北的铁栏杆是甜的呢[妙啊][doge]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  1573,
  FROM_UNIXTIME(1763976190)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  101,  -- ✅ 修改：BV1FQUrBzE2P -> 101
  '44745161',
  '阿斯嘉德三公主',
  'https://i2.hdslb.com/bfs/face/37001a8c716ba4904e68de9d84f42be23bb4bf68.jpg',
  '等我老了我也要胡说八道',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  3452,
  FROM_UNIXTIME(1763979311)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('483879799', '猛男舞团IconX', 'https://i1.hdslb.com/bfs/face/48503596681ab931d7d08a34071d3c8bf8b284bb.webp')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  102,  -- ✅ 修改：BV1AM4y1M71p -> 102
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%A0%B4%E4%BA%BF%E7%BA%AA%E5%BF%B5%21%E3%80%90%E7%8C%9B%E7%94%B7%E7%89%88%E3%80%91%E6%96%B0%E5%AE%9D%E5%B2%9B%204K%E9%AB%98%E6%B8%85%E9%87%8D%E7%BD%AE%E5%8A%A0%E5%BC%BA%E7%89%88.mp4',
  'http://i0.hdslb.com/bfs/archive/f28fae0e133f8bb289d719e031891764edb332e1.jpg',
  '破亿纪念!【猛男版】新宝岛 4K高清重置加强版',
  '[]',
  116000,
  'LANDSCAPE',
  '483879799',
  '猛男舞团IconX',
  'https://i1.hdslb.com/bfs/face/48503596681ab931d7d08a34071d3c8bf8b284bb.webp',
  3204017, 47621, 1084135, 610039, 51075881,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  102,  -- ✅ 修改：BV1AM4y1M71p -> 102
  '284208940',
  '卡涅棘刺',
  'https://i1.hdslb.com/bfs/face/886ded3c03bf32bae9def6027539d9b7a83eea25.jpg',
  '1.14版本主要修改:
.增强了画质
.移除了领舞的项链
.增长了领舞的头发长度
.胖哥 头发颜色更变
.别的不知道了',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  228726,
  FROM_UNIXTIME(1625533567)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  102,  -- ✅ 修改：BV1AM4y1M71p -> 102
  '394196223',
  '九月贰拾陆',
  'http://i0.hdslb.com/bfs/face/0b2b12c9faa9fca9107472ec99903264c7f48e6e.jpg',
  '男  人  大  可  不  必  这  么  完  美[doge][doge]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  157836,
  FROM_UNIXTIME(1625493967)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  102,  -- ✅ 修改：BV1AM4y1M71p -> 102
  '429532543',
  '无影ya',
  'https://i1.hdslb.com/bfs/face/481dd54620a70415a965ce03013e7f9e1c5d7b79.jpg',
  '主C真的帅，自信的眼神，严肃的表情，舞蹈人般的身材，以及节奏非常强烈的肥肉抖动和收吸，加上浪子一样的发型，天王的崛起。[响指]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  119404,
  FROM_UNIXTIME(1625488332)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('11831050', 'RedialC', 'https://i1.hdslb.com/bfs/face/21eb08b4bc9835218ba9f1c2ebd152060f864f34.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  103,  -- ✅ 修改：BV19T411T7Yj -> 103
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E3%80%904K60P%E7%AB%96%E5%B1%8F%E4%B8%A8%E8%99%9A%E5%B9%BB5%E3%80%91%E6%83%B3%E6%8B%A5%E4%BD%A0%E5%9C%A8%E6%80%80%2C%E5%BF%90%E5%BF%91%E7%9D%80%E5%9C%B0%E7%AD%89%E5%BE%85%E4%BD%A0%E6%9D%A5%7B%E7%AB%96%E5%B1%8F%E9%87%8D%E7%BD%AE%E7%89%88%7D.mp4',
  'http://i2.hdslb.com/bfs/archive/f2993ef27f3f669802a7fe024e1ca83061991b9d.jpg',
  '【4K60P竖屏丨虚幻5】想拥你在怀,忐忑着地等待你来{竖屏重置版}',
  '[]',
  169000,
  'PORTRAIT',
  '11831050',
  'RedialC',
  'https://i1.hdslb.com/bfs/face/21eb08b4bc9835218ba9f1c2ebd152060f864f34.jpg',
  10255, 292, 7353, 377, 71685,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  103,  -- ✅ 修改：BV19T411T7Yj -> 103
  '516003403',
  '诸葛黛玉倒拔紫金葫芦',
  'https://i1.hdslb.com/bfs/face/f259fe15491eb86731a4bf0b5ceba14b69c3a09d.jpg',
  '大家可以试试跟着天依一起眨眼，这感觉就好像是天依在工作(录舞蹈视频)时看到你了，但舞不能停，就眨眨眼向你打个招呼，而你也眨眼回意。但跟着天依眨眼的次数多了，就好像情侣之间无事可做，看着对方的脸逗对方笑，结果一个视频下来给我看脸红了♡(๑•ω•๑)♡[tv_微笑]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  655,
  FROM_UNIXTIME(1663973694)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  103,  -- ✅ 修改：BV19T411T7Yj -> 103
  '500332621',
  '理五晨曦',
  'https://i1.hdslb.com/bfs/face/5e60ad2184bf18ea01965d4e8db98184e125ccac.jpg',
  '不知道为什么
感觉好行，
快乐！这模型做的好细
看得......
_(≧∇≦」∠)_',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  91,
  FROM_UNIXTIME(1663988623)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  103,  -- ✅ 修改：BV19T411T7Yj -> 103
  '12746344',
  'SpeedyWing',
  'https://i1.hdslb.com/bfs/face/84a53815c6098d0572f4eeff5eac8fc479738a32.jpg',
  '[脱单doge]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  61,
  FROM_UNIXTIME(1672834185)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('523064213', '海外视频', 'https://i2.hdslb.com/bfs/face/ecb0696c509831173929547693babd135b5b8867.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  104,  -- ✅ 修改：BV1HLdUYpE81 -> 104
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E2%80%9C%E4%BA%BA%E7%94%9F%E8%8B%A6%E7%9F%AD%20%E5%8F%8A%E6%97%B6%E8%A1%8C%E4%B9%90%22.mp4',
  'http://i0.hdslb.com/bfs/archive/1b4cb7873b52ebca98c08f8370702f3b7346cc92.jpg',
  '“人生苦短 及时行乐"',
  '[]',
  89000,
  'PORTRAIT',
  '523064213',
  '海外视频',
  'https://i2.hdslb.com/bfs/face/ecb0696c509831173929547693babd135b5b8867.jpg',
  3304, 31, 1160, 56, 23699,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  104,  -- ✅ 修改：BV1HLdUYpE81 -> 104
  '3546705091889223',
  '我凶的嘞',
  'https://i0.hdslb.com/bfs/face/de045acf59df5a1eaccd059782ef2824337a561e.jpg',
  '你只看到我的表面贫瘠，没看到我的内心丰盈[星星眼]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  29,
  FROM_UNIXTIME(1745634415)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  104,  -- ✅ 修改：BV1HLdUYpE81 -> 104
  '3546750843358055',
  '放弃取名字了龠字号',
  'https://i2.hdslb.com/bfs/face/d7fff48a5e921072f520070ad818735cf73e7577.jpg',
  '有一种笨拙而自由的快乐',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  29,
  FROM_UNIXTIME(1744386935)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  104,  -- ✅ 修改：BV1HLdUYpE81 -> 104
  '501937905',
  '爱上了你没什么道裡',
  'https://i0.hdslb.com/bfs/face/1a9ed8a15f65cc75674371ac7ca82ec4bdccda3e.jpg',
  '没有人认出来吗，这是怪哥的张牙舞爪健身操啊[笑哭]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  26,
  FROM_UNIXTIME(1744445594)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('2407474', '液老板', 'https://i1.hdslb.com/bfs/face/67cf24304f29b4addda4938b8bb0ee98cdd8e12f.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  105,  -- ✅ 修改：BV1qaQxYBESr -> 105
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%81%8B%E4%B8%8E%E6%B7%B1%E7%A9%BA%E4%B8%A8%E7%8E%8B%E7%89%8CACE%E5%A4%8F%E4%BB%A5%E6%98%BC%E4%B8%AA%E4%BA%BAsolo%E9%A6%96%E7%A7%80%E6%83%8A%E8%89%B3%E4%BA%AE%E7%9B%B8%E2%9D%A4%E2%9D%A4%E2%9D%A4%E4%B8%A8%E7%AB%96%E5%B1%8F%E7%89%88.mp4',
  'http://i1.hdslb.com/bfs/archive/1d83c52953c2c7730156452d94e13fe9e18dc045.jpg',
  '恋与深空丨王牌ACE夏以昼个人solo首秀惊艳亮相❤❤❤丨竖屏版',
  '[]',
  42000,
  'PORTRAIT',
  '2407474',
  '液老板',
  'https://i1.hdslb.com/bfs/face/67cf24304f29b4addda4938b8bb0ee98cdd8e12f.jpg',
  40326, 959, 9433, 1256, 250551,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  105,  -- ✅ 修改：BV1qaQxYBESr -> 105
  '1762948',
  '烁砾',
  'https://i1.hdslb.com/bfs/face/5dc022ad00f5ef7c0d2633176beedfdac64c0b09.jpg',
  '《冷 脸 热 舞 王》',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  5130,
  FROM_UNIXTIME(1742109056)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  105,  -- ✅ 修改：BV1qaQxYBESr -> 105
  '2407474',
  '液老板',
  'https://i1.hdslb.com/bfs/face/67cf24304f29b4addda4938b8bb0ee98cdd8e12f.jpg',
  'xhs那边今天被创了n次没想到B站这边也有觉得怪或者好笑的评论…真的心累了真的…把那边的解释复制过来一下……真的…姐们觉得好笑或者搞笑或者面部表情不够生动的请刷走可以吗？？？我做梦也想做的跟叠纸一样好啊但是个人能力仅限于此了
解释一下动作的事情：mmd视频的动作数据大部分是业余爱好者用爱发电免费分享的，那种很流畅的动捕数据也不多大部分是女团舞（比如有些虚拟爱豆宣传 公司会放出自掏腰包做的动捕数据免费分享）帅气的男舞真的很少很少，肯定和叠纸专业动画师打磨出作品不是一个水平，所以大家不喜欢可以刷走吗动作虽然不是我K的但是看了也会伤心的…毕竟帅气的男舞数据真的很少做一个少一个的程度…',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  2574,
  FROM_UNIXTIME(1742136700)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  105,  -- ✅ 修改：BV1qaQxYBESr -> 105
  '264513499',
  '刀鸣集',
  'https://i1.hdslb.com/bfs/face/8e8db0582ff5d4a6e90c06b40c643764a82adda7.jpg',
  '摸摸，up主没深度混过深空的社区吧……社区环境打架打厨子打得很厉害的，夏以昼作为最晚上线的男主被盯着打压是很正常的[笑哭]我不玩乙游仅仅是吃瓜，之前围观到这个男主的同人作者也有被一些不知道该怎么形容的其他男主的粉丝围攻的，然后会有很多装作是这个男主的推的披皮在搬弄是非，建议老师别太放在心上，作为老二次元来看你的mmd真的做得挺好的。',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  1724,
  FROM_UNIXTIME(1742227525)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('296462800', '与优树', 'https://i1.hdslb.com/bfs/face/a15c18d253ca4f368064da9b5caf12860aedd005.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  106,  -- ✅ 修改：BV1EGUrBKEmM -> 106
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E3%80%904k%E7%AB%96%E5%B1%8F%E3%80%91%E4%B8%AD%E9%87%8E%E6%A2%93%E7%9A%84%E4%B8%8D%E5%BF%83%E5%8A%A8%E6%8C%91%E6%88%98%E5%93%A6%EF%BC%81%EF%BC%81%EF%BC%81%EF%BC%81.mp4',
  'http://i0.hdslb.com/bfs/archive/741967a2f53a713d77a9d7b6b37d2cf6860fd634.jpg',
  '【4k竖屏】中野梓的不心动挑战哦！！！！',
  '[]',
  55000,
  'PORTRAIT',
  '296462800',
  '与优树',
  'https://i1.hdslb.com/bfs/face/a15c18d253ca4f368064da9b5caf12860aedd005.jpg',
  1249, 54, 317, 39, 3503,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  106,  -- ✅ 修改：BV1EGUrBKEmM -> 106
  '695488101',
  '绝非鳝类o',
  'https://i0.hdslb.com/bfs/face/4bbcdc27f3b227322c87bb5cb30600c371828e73.jpg',
  '[打call][打call]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  7,
  FROM_UNIXTIME(1763996710)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  106,  -- ✅ 修改：BV1EGUrBKEmM -> 106
  '84511479',
  '初音太萌',
  'https://i0.hdslb.com/bfs/face/f22d4da7b46c34c572c72f0d7ede76d34004d5ea.jpg',
  '@南望书斋',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  1,
  FROM_UNIXTIME(1764069270)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  106,  -- ✅ 修改：BV1EGUrBKEmM -> 106
  '3546745751472938',
  '相遇的寻',
  'https://i2.hdslb.com/bfs/face/d19a97fdd3430fab5df61d7ada433ae3a38b8eef.jpg',
  '见K-ON必赞[星星眼][星星眼][给心心]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  2,
  FROM_UNIXTIME(1764034685)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('3546708495567018', 'jagaimotatop', 'https://i0.hdslb.com/bfs/face/6decfb735be6078999a0c330b1b56b324694e873.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  107,  -- ✅ 修改：BV1ufmTYYE1i -> 107
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E7%9F%AD%E7%89%87%E4%B8%A8%E4%BA%BA%E7%B1%BB%E4%B8%BA%E4%BD%95%E8%BF%B7%E6%81%8B%E5%A4%8F%E5%A4%A9.mp4',
  'http://i2.hdslb.com/bfs/archive/7593f88eb2ee387181ccf2fb7c646f515cee9f35.jpg',
  '竖屏短片丨人类为何迷恋夏天',
  '[]',
  91000,
  'PORTRAIT',
  '3546708495567018',
  'jagaimotatop',
  'https://i0.hdslb.com/bfs/face/6decfb735be6078999a0c330b1b56b324694e873.jpg',
  1198, 30, 933, 126, 13279,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  107,  -- ✅ 修改：BV1ufmTYYE1i -> 107
  '11822935',
  '银色亮片',
  'https://i1.hdslb.com/bfs/baselabs/b84a4cb7eb0dc883e67c4766603e6c5b1e58421e.png',
  '才刚开始降温我就要怀念起盛夏了[大哭]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  23,
  FROM_UNIXTIME(1729545571)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  107,  -- ✅ 修改：BV1ufmTYYE1i -> 107
  '96640975',
  '蛀牙奶昔丷',
  'https://i1.hdslb.com/bfs/face/816dc5224a4f5f2a1b351f6e18e4cd247ea4093b.jpg',
  '夏天有种生命力。',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  20,
  FROM_UNIXTIME(1730520114)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  107,  -- ✅ 修改：BV1ufmTYYE1i -> 107
  '225444889',
  'YourF4u1t',
  'https://i1.hdslb.com/bfs/face/ba268a6647eac4a07570e9e1b976512986951782.jpg',
  '为什么呢',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  17,
  FROM_UNIXTIME(1729832863)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('15377173', '烟季', 'https://i1.hdslb.com/bfs/face/3c657487f9a7993f50bcbafa82f64f99ff1229bd.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  108,  -- ✅ 修改：BV1sh1NBiEXq -> 108
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E2%80%9C%E5%91%8A%E8%AF%89%E6%88%91%EF%BC%8C%E7%A5%9E%E4%BC%9A%E6%B5%81%E8%A1%80%E5%90%97%EF%BC%9F%E2%80%9D%E3%80%90%F0%9D%90%84%F0%9D%90%95%F0%9D%90%80%E3%80%91%F0%9D%90%8B%F0%9D%96%BE%F0%9D%97%8D%20%F0%9D%9A%B0%F0%9D%97%8D%20%F0%9D%90%81%E1%A5%A3%F0%9D%96%BE%F0%9D%96%BE%E1%91%AF.mp4',
  'http://i1.hdslb.com/bfs/archive/f571da4a5f781cd553c262a25c1f427a822f799e.jpg',
  '“告诉我，神会流血吗？”【𝐄𝐕𝐀】𝐋𝖾𝗍 𝚰𝗍 𝐁ᥣ𝖾𝖾ᑯ',
  '[]',
  120000,
  'LANDSCAPE',
  '15377173',
  '烟季',
  'https://i1.hdslb.com/bfs/face/3c657487f9a7993f50bcbafa82f64f99ff1229bd.jpg',
  11368, 147, 5155, 292, 97432,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  108,  -- ✅ 修改：BV1sh1NBiEXq -> 108
  '377572568',
  '周末酱想睡觉',
  'https://i2.hdslb.com/bfs/face/0a8e6fbd90335c5de2235731f9e9b9b617bdef11.jpg',
  'on last kiss 响起时候真哭了[Mygo表情包_大哭][Mygo表情包_大哭][Mygo表情包_大哭]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  167,
  FROM_UNIXTIME(1761889794)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  108,  -- ✅ 修改：BV1sh1NBiEXq -> 108
  '501483',
  'kurundam',
  'https://i0.hdslb.com/bfs/face/3e9d09fccb015a00a3057f2b0f9f6ca096b31377.jpg',
  '神会说：欧咩得多',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  147,
  FROM_UNIXTIME(1761885542)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  108,  -- ✅ 修改：BV1sh1NBiEXq -> 108
  '104243347',
  '繁霜星落',
  'https://i1.hdslb.com/bfs/face/d196dd031dc8ceb6b328812aacdaf441d28656dc.jpg',
  '和女朋友一起去的，我俩包场了[呲牙]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  74,
  FROM_UNIXTIME(1761919033)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('3546610069932716', 'JN-yxn', 'https://i2.hdslb.com/bfs/face/49fee719f87cc10f56bd3d280b53bf92aa2b9f40.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  109,  -- ✅ 修改：BV1eixNzWE4B -> 109
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E5%BD%93%E8%81%8C%E9%AB%98%E7%94%9F%E5%B0%9D%E8%AF%95%E6%A8%A1%E4%BB%BF%E8%8B%B9%E6%9E%9C%E5%8F%91%E5%B8%83%E4%BC%9A%E5%BC%80%E5%9C%BA%E7%BA%BF%E6%9D%A1%E5%8A%A8%E7%94%BB%E2%80%A6%E2%80%A6.mp4',
  'http://i2.hdslb.com/bfs/archive/0a0bf8a32bb44ba9339a409edefff9920c5dfe80.jpg',
  '当职高生尝试模仿苹果发布会开场线条动画……',
  '[]',
  50000,
  'LANDSCAPE',
  '3546610069932716',
  'JN-yxn',
  'https://i2.hdslb.com/bfs/face/49fee719f87cc10f56bd3d280b53bf92aa2b9f40.jpg',
  50839, 1083, 8477, 1018, 655854,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  109,  -- ✅ 修改：BV1eixNzWE4B -> 109
  '148957839',
  '隔壁闲人',
  'https://i0.hdslb.com/bfs/face/733d8422f24f91fda9a95b6097a693dda2de1d24.jpg',
  '目前 B 站看到最用心的一个，花了不少功夫吧',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  8420,
  FROM_UNIXTIME(1761542269)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  109,  -- ✅ 修改：BV1eixNzWE4B -> 109
  '389685337',
  '浪味鲜虾片',
  'https://i1.hdslb.com/bfs/face/b28a351b7deea124b98c24486a2bb3c4a2deac0b.jpg',
  '只要做的足够诚意，瑕疵就不会成为减分项，因为那叫成长空间。',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  5786,
  FROM_UNIXTIME(1761683544)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  109,  -- ✅ 修改：BV1eixNzWE4B -> 109
  '379390855',
  '瓦尔登湖上的丫丫',
  'https://i1.hdslb.com/bfs/face/1cc39f40dfe0ae89270e7c9b4e4063ee5e348dda.jpg',
  '谁说的校园跑路线[无语]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  3754,
  FROM_UNIXTIME(1761643204)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('598581575', '汤小圆鸽鸽鸽', 'https://i2.hdslb.com/bfs/face/8dcf9575286a3a6ae8b124d4b38c082c208d8a8e.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  110,  -- ✅ 修改：BV1ym411B764 -> 110
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E3%80%90%E7%A0%82%E9%87%91%E3%80%91Trouble%20Maker.mp4',
  'http://i1.hdslb.com/bfs/archive/199d90c7c38067825e0efed2d5d0b0ce8a2f0bb5.jpg',
  '【砂金】Trouble Maker',
  '[]',
  26000,
  'LANDSCAPE',
  '598581575',
  '汤小圆鸽鸽鸽',
  'https://i2.hdslb.com/bfs/face/8dcf9575286a3a6ae8b124d4b38c082c208d8a8e.jpg',
  30415, 135, 34213, 490, 517941,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  110,  -- ✅ 修改：BV1ym411B764 -> 110
  '3494361033608001',
  '偉yuan',
  'https://i0.hdslb.com/bfs/face/f9963184a9bb5e65d75a04d7fc7a3ffcb33c5a0e.jpg',
  '我发现崩铁男角色老有魅力了，原神厨女角色多，崩铁厨男角色多[doge]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  1361,
  FROM_UNIXTIME(1713027683)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  110,  -- ✅ 修改：BV1ym411B764 -> 110
  '519801151',
  '就是救世啊',
  'https://i1.hdslb.com/bfs/face/9df3c067507afcc99ad540bd9fdc38bb766c39cb.webp',
  '好耶，老师剪的好棒，这首歌感觉真的超适合砂金大人！[初音未来三连快乐表情包_三连快乐][初音未来三连快乐表情包_三连快乐][初音未来三连快乐表情包_三连快乐]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  150,
  FROM_UNIXTIME(1713024463)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  110,  -- ✅ 修改：BV1ym411B764 -> 110
  '598581575',
  '汤小圆鸽鸽鸽',
  'https://i2.hdslb.com/bfs/face/8dcf9575286a3a6ae8b124d4b38c082c208d8a8e.jpg',
  '砂金新作品！！BV1At42177Pe[星星眼]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  68,
  FROM_UNIXTIME(1713795906)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('652239032', 'IGN中国', 'https://i2.hdslb.com/bfs/face/4c9095a6fc7d6ef7bf97ee1c65767f537763c60c.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  111,  -- ✅ 修改：BV1pCyqB9EYz -> 111
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E3%80%90IGN%E3%80%91%E7%94%B5%E5%BD%B1%E3%80%8A%E7%96%AF%E7%8B%82%E5%8A%A8%E7%89%A9%E5%9F%8E2%E3%80%8B%E5%85%A8%E6%96%B0%E9%A2%84%E5%91%8A.mp4',
  'http://i0.hdslb.com/bfs/archive/f6ce8f38d63e639e0ab7f937b6a7405e97886df8.jpg',
  '【IGN】电影《疯狂动物城2》全新预告',
  '[]',
  135000,
  'LANDSCAPE',
  '652239032',
  'IGN中国',
  'https://i2.hdslb.com/bfs/face/4c9095a6fc7d6ef7bf97ee1c65767f537763c60c.jpg',
  25744, 1594, 4025, 10614, 1059347,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  111,  -- ✅ 修改：BV1pCyqB9EYz -> 111
  '360606596',
  '东方仗仗助',
  'https://i2.hdslb.com/bfs/face/4c324330f2e1f76b1d72d780d8908cf94d1a8a88.jpg',
  'OK了，官方还特意把尼克救溺水朱迪的画面闪了三回，很难让人不注意，是搭档是情侣我自有定夺[doge]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  2888,
  FROM_UNIXTIME(1761621602)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  111,  -- ✅ 修改：BV1pCyqB9EYz -> 111
  '3546390502312149',
  'bili_96838051560',
  'https://i0.hdslb.com/bfs/face/member/noface.jpg',
  '[doge][思考]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  1910,
  FROM_UNIXTIME(1761653215)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  111,  -- ✅ 修改：BV1pCyqB9EYz -> 111
  '20301524',
  'Mousquetaire',
  'https://i2.hdslb.com/bfs/face/dfa3dc4b52039c86774687bf7b49428a8c175958.jpg',
  '注意到时候电影票别买成国语版[doge]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  4106,
  FROM_UNIXTIME(1761620519)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('669334488', '环球音乐中国', 'https://i0.hdslb.com/bfs/face/11ad74a0cf807b9896656ef9987e1b833aa11f8a.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  112,  -- ✅ 修改：BV1dD4y1o76G -> 112
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/Beauty%20And%20A%20Beat%20-%20Justin%20Bieber.mp4',
  'http://i2.hdslb.com/bfs/archive/50a30a69df13a706cd077814fb01cc6b440bd54e.jpg',
  'Beauty And A Beat - Justin Bieber',
  '[]',
  294000,
  'LANDSCAPE',
  '669334488',
  '环球音乐中国',
  'https://i0.hdslb.com/bfs/face/11ad74a0cf807b9896656ef9987e1b833aa11f8a.jpg',
  573, 17, 499, 175, 32214,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  112,  -- ✅ 修改：BV1dD4y1o76G -> 112
  '213820482',
  '王高徐',
  'https://i0.hdslb.com/bfs/face/2b4cfc2b6671ff201d7e69f975f268e1db47655b.jpg',
  '这（MV)莫名想到，那个‘吹牛老爹’的事件',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  28,
  FROM_UNIXTIME(1731223971)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  112,  -- ✅ 修改：BV1dD4y1o76G -> 112
  '499471102',
  '追丶者',
  'https://i1.hdslb.com/bfs/face/f089e0bae1801e859caae60f1667e04b7337a315.jpg',
  '怎么没人',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  4,
  FROM_UNIXTIME(1757747862)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  112,  -- ✅ 修改：BV1dD4y1o76G -> 112
  '3461567819549658',
  '发鬓',
  'https://i0.hdslb.com/bfs/face/fd8681f451ed967e615f3632f72b9525737bb266.jpg',
  '有种经济上行的感觉',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  2,
  FROM_UNIXTIME(1764442867)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('92850496', '变形菌纲', 'https://i1.hdslb.com/bfs/face/01b1fc94d9f2654c291f0634cc024d6eed12ed6a.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  113,  -- ✅ 修改：BV1o5SsBXEyc -> 113
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%8C%ABmeme%E7%BE%8E%E9%A3%9F%EF%BD%9C%E8%A2%AB%E7%AA%9D%E5%8F%98%E6%88%90%E5%8F%AF%E4%B8%BD%E9%A5%BC%E4%BA%86%E5%96%B5.mp4',
  'http://i2.hdslb.com/bfs/archive/a285a8559781fed7cd3a0664af2ba58fef9f59f9.jpg',
  '猫meme美食｜被窝变成可丽饼了喵',
  '[]',
  100000,
  'PORTRAIT',
  '92850496',
  '变形菌纲',
  'https://i1.hdslb.com/bfs/face/01b1fc94d9f2654c291f0634cc024d6eed12ed6a.jpg',
  34256, 285, 2673, 1369, 194806,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  113,  -- ✅ 修改：BV1o5SsBXEyc -> 113
  '3546771223481092',
  '巧合很奇妙',
  'https://i1.hdslb.com/bfs/face/192983acc366c0e035bc3dbdce74120641e9cdcc.jpg',
  '好可爱QWQ心血来潮画了一下',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  1961,
  FROM_UNIXTIME(1764684296)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  113,  -- ✅ 修改：BV1o5SsBXEyc -> 113
  '1326922374',
  '藏狐108',
  'https://i0.hdslb.com/bfs/face/84739ad43064d322acdfe5389593a33f51087686.jpg',
  '求做这个',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  890,
  FROM_UNIXTIME(1764668875)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  113,  -- ✅ 修改：BV1o5SsBXEyc -> 113
  '693576322',
  'AAA灰珀晶片星琼批发',
  'https://i1.hdslb.com/bfs/face/1b9d5c5f9f7ffe5be04241702610db6e063460ad.jpg',
  '菌老师，求这个[打call]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  389,
  FROM_UNIXTIME(1764684134)
);

INSERT INTO beatu_users (id, nickname, avatar)
VALUES ('291894729', '火大花haha', 'https://i0.hdslb.com/bfs/face/8fa1c5a5d8e23ae1bec7ef07e132daa7f08e5f62.jpg')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  avatar   = VALUES(avatar);

INSERT INTO beatu_videos (
  id, play_url, cover_url, title,
  tags, duration_ms, orientation,
  author_id, author_name, author_avatar,
  like_count, comment_count, favorite_count, share_count, view_count,
  qualities
)
VALUES (
  114,  -- ✅ 修改：BV1AzS3BFEKq -> 114
  'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E2%9D%A4%E6%88%91%20%E5%BE%88%20%E5%8F%AF%20%E7%88%B1%20%E8%AF%B7%20%E7%BB%99%20%E6%88%91%20%E9%92%B1%E2%9D%A4.mp4',
  'http://i0.hdslb.com/bfs/archive/d1c8764ca97c9706f628753c56f3151c8e6cf719.jpg',
  '❤我 很 可 爱 请 给 我 钱❤',
  '[]',
  196000,
  'LANDSCAPE',
  '291894729',
  '火大花haha',
  'https://i0.hdslb.com/bfs/face/8fa1c5a5d8e23ae1bec7ef07e132daa7f08e5f62.jpg',
  64218, 961, 27095, 2262, 917923,
  '[]'
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  114,  -- ✅ 修改：BV1AzS3BFEKq -> 114
  '179319934',
  '双海包子铺老板娘',
  'https://i1.hdslb.com/bfs/face/6e1da67e693a19cc9d468a21e2c8bd30fe05c9b7.jpg',
  '[妙啊]虽然模仿的很像，但是感觉没有很好的模仿出她对于钱如痴如狂的情感呢',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  107,
  FROM_UNIXTIME(1764390768)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  114,  -- ✅ 修改：BV1AzS3BFEKq -> 114
  '1069344105',
  'XU_Beft',
  'https://i0.hdslb.com/bfs/face/b461e0beb28bdeda355a4919b3a1c7b24a6ec8db.jpg',
  '歌词大意：来财，来，来财[doge]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  1113,
  FROM_UNIXTIME(1764383515)
);

INSERT INTO beatu_comments (
  video_id, author_id, author_name, author_avatar,
  content, parent_id, is_ai_reply, ai_model, ai_source, ai_confidence,
  like_count, created_at
)
VALUES (
  114,  -- ✅ 修改：BV1AzS3BFEKq -> 114
  '176618655',
  '你家的大湿兄',
  'http://i0.hdslb.com/bfs/face/0e8212a0b68957b400fbaae570f58c686eebc205.jpg',
  '原版是老阿姨装嫩，这个是小可爱装老阿姨。非常有意思[doge_金箍]',
  NULL,
  FALSE,
  NULL, NULL, NULL,
  2877,
  FROM_UNIXTIME(1764391082)
);
