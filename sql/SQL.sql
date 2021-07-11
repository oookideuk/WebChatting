#member 테이블 생성
create table member(
	member_id varchar(20) primary key not null,
	password varchar(512) not null,
    name varchar(20) not null,
    role varchar(20) not null,
    email varchar(512) not null,
    email_auth_key varchar(512) not null,
    email_auth_flag int not null DEFAULT 0,
    register_date timestamp(6) DEFAULT CURRENT_TIMESTAMP(6),
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

# refresh_token 테이블 생성
create table jwt_token(
	token_id varchar(256) primary key,
    expiration_date timestamp(6) not null,
	refresh_count int not null default 0,
    member_id varchar(20) not null,
    
    foreign key(member_id) references member(member_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

#profile_picture 테이블 생성
create table profile_picture(
	file_id bigint primary key not null auto_increment,
    stored_file_name varchar(255) not null,
    original_file_name varchar(255) not null,
    size bigint not null,
    upload_path varchar(255) not null,
    register_date timestamp(6) not null,
    member_id varchar(20) not null,
    
    foreign key(member_id) references member(member_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

#chat_room 테이블 생성
create table chat_room(
	room_id varchar(100) primary key,
    title varchar(255) comment '제목',
    created_date timestamp(6) DEFAULT CURRENT_TIMESTAMP(6)
    
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table chat_room_hash_tag(
	hash_tag_id bigint primary key auto_increment,
    hash_tag varchar(100),
    room_id varchar(100)
    
    #foreign key(room_id) references chat_room(room_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table chat_file(
    file_id bigint primary key auto_increment,
    type varchar(50),
    stored_file_name varchar(255),
    original_file_name varchar(255),
    size bigint,
    upload_path varchar(255),
    created_date timestamp(6) DEFAULT CURRENT_TIMESTAMP(6),
    room_id varchar(100),
    participant_id varchar(20)
    
    #foreign key(room_id) references chat_room(room_id)
    #foreign key(participant_id) references chat_participant(participant_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table chat_message(
    message_id bigint primary key auto_increment,
    type varchar(20),
    message text,
    created_date timestamp(6) DEFAULT CURRENT_TIMESTAMP(6),
    room_id varchar(100),
    sender varchar(20)
    
    #foreign key(room_id) references chat_room(room_id)
    #foreign key(sender) references chat_participant(participant_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table chat_message_attach(
    message_attach_id bigint primary key auto_increment,
    url varchar(255),
    message_id bigint
    
    #foreign key(message_id) references chat_message(message_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

#chat_participant 테이블 생성
create table chat_participant(
    room_id varchar(100),
    participant_id varchar(20),
    entry_date timestamp(6) DEFAULT CURRENT_TIMESTAMP(6),
    offline_date timestamp(6) DEFAULT CURRENT_TIMESTAMP(6),
    
    primary key (room_id, participant_id)
    #foreign key(room_id) references chat_room(room_id),
    #foreign key(participant_id) references member(member_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table chat_online_participant(
    session_id varchar(100),
    host_address varchar(100),
    port int,
    room_id varchar(100),
    participant_id varchar(20),
    entry_date timestamp(6) DEFAULT CURRENT_TIMESTAMP(6),
    
    primary key(session_id, host_address, port)
    #foreign key(room_id) references chat_room(room_id)
    #foreign key(participant_id) references chat_participant(participant_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

show tables;
